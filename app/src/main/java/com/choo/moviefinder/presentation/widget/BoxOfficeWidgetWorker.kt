package com.choo.moviefinder.presentation.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.choo.moviefinder.domain.usecase.GetDailyBoxOfficeWithTmdbMatchUseCase
import com.choo.moviefinder.domain.usecase.GetWeeklyBoxOfficeWithTmdbMatchUseCase
import com.choo.moviefinder.presentation.home.BoxOfficePeriod
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * 박스오피스 위젯의 데이터 갱신 담당 Worker.
 *
 * "네트워크는 Worker가, 렌더링은 Glance가" 분리하는 표준 패턴 — 여기서 받아온 결과를 Glance state에
 * 스냅샷으로 기록한 뒤 [BoxOfficeWidget.updateAll]로 재구성을 유발한다.
 */
class BoxOfficeWidgetWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    // hilt-work(@HiltWorker)를 새로 들이지 않고 기존 위젯(WatchGoalWidget)과 동일하게 EntryPoint로 의존성을 얻는다.
    // Presentation → Data 직접 참조 금지 규칙에 따라 UseCase만 노출한다 (DAO/Repository 구현 노출 금지).
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BoxOfficeWidgetEntryPoint {
        fun getDailyBoxOfficeWithTmdbMatchUseCase(): GetDailyBoxOfficeWithTmdbMatchUseCase
        fun getWeeklyBoxOfficeWithTmdbMatchUseCase(): GetWeeklyBoxOfficeWithTmdbMatchUseCase
    }

    override suspend fun doWork(): Result {
        val glanceIds = GlanceAppWidgetManager(applicationContext)
            .getGlanceIds(BoxOfficeWidget::class.java)

        // 배치된 위젯이 없으면 갱신할 대상이 없다 — 예약된 작업까지 정리하고 끝낸다.
        if (glanceIds.isEmpty()) {
            cancelAll(applicationContext)
            return Result.success()
        }

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            BoxOfficeWidgetEntryPoint::class.java
        )

        // 위젯 인스턴스마다 선택된 기간이 다를 수 있어(헤더 배지 토글), 각자 자신의 기간만 갱신한다.
        // 인스턴스 하나가 실패해도 나머지는 계속 갱신하되, 하나라도 실패하면 WorkManager 재시도를 유도한다.
        val anyFailure = glanceIds.map { glanceId ->
            val period = getAppWidgetState(applicationContext, PreferencesGlanceStateDefinition, glanceId)
                .readPeriod()
            refreshGlanceId(glanceId, period, entryPoint)
        }.any { succeeded -> !succeeded }

        BoxOfficeWidget().updateAll(applicationContext)
        return when {
            !anyFailure -> Result.success()
            runAttemptCount < MAX_RUN_ATTEMPTS -> Result.retry()
            else -> Result.failure()
        }
    }

    /** @return 갱신 성공 여부. */
    private suspend fun refreshGlanceId(
        glanceId: GlanceId,
        period: BoxOfficePeriod,
        entryPoint: BoxOfficeWidgetEntryPoint
    ): Boolean {
        try {
            val snapshot = buildSnapshot(
                period = period,
                dailyUseCase = entryPoint.getDailyBoxOfficeWithTmdbMatchUseCase(),
                weeklyUseCase = entryPoint.getWeeklyBoxOfficeWithTmdbMatchUseCase(),
                nowMillis = System.currentTimeMillis()
            )
            // 빈 리스트는 에러가 아니다(집계 전 시간대 등) — 빈 스냅샷을 그대로 저장한다.
            writeWidgetState(applicationContext, glanceId, period, snapshot, hasError = false)
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "위젯: 박스오피스 갱신 실패 (period=$period)")
            // 기존 스냅샷은 지우지 않는다 — 갱신 실패해도 마지막 성공 데이터를 계속 보여준다.
            writeWidgetState(applicationContext, glanceId, period, snapshot = null, hasError = true)
            return false
        }
    }

    companion object {
        const val PERIODIC_WORK_NAME = "box_office_widget_periodic"
        const val ONE_TIME_WORK_NAME = "box_office_widget_refresh"

        private const val MAX_RUN_ATTEMPTS = 2
        private const val REFRESH_INTERVAL_HOURS = 6L
        private const val BACKOFF_DELAY_MINUTES = 10L

        private val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<BoxOfficeWidgetWorker>(
                REFRESH_INTERVAL_HOURS,
                TimeUnit.HOURS
            )
                .setConstraints(networkConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES)
                .build()

            // KEEP 필수: REPLACE를 쓰면 onUpdate가 호출될 때마다 주기가 리셋돼 영영 실행되지 않을 수 있다.
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /**
         * REPLACE 필수: 새로고침 버튼/기간 토글은 모두 "지금 당장 새로 받아오라"는 명시적 사용자 의도다.
         * KOFIC 응답 지연 등으로 이전 시도가 지수 백오프 대기 중일 때 KEEP을 쓰면, 그 대기(최대 수십 분)가
         * 끝날 때까지 새 시도가 아예 예약되지 않아 버튼을 눌러도 실기기에서 아무 반응이 없는 것처럼 보인다
         * (2026-09-17 실기기 SM-S926N 검증에서 발견: 설치 직후 타임아웃 → 이후 토글을 눌러도 dumpsys
         * jobscheduler상 다음 실행이 +6분 뒤로 밀려 있었다). REPLACE는 그 대기를 취소하고 즉시 새 시도를 건다.
         */
        fun enqueueOneTimeRefresh(context: Context) {
            val request = OneTimeWorkRequestBuilder<BoxOfficeWidgetWorker>()
                .setConstraints(networkConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancelAll(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            workManager.cancelUniqueWork(ONE_TIME_WORK_NAME)
        }

        /**
         * UseCase 호출 → 위젯 스냅샷 변환까지의 순수 로직.
         * Android static API에 묶이지 않아 JVM 유닛 테스트로 성공/빈 리스트/예외 전파를 검증할 수 있다.
         */
        internal suspend fun buildSnapshot(
            period: BoxOfficePeriod,
            dailyUseCase: GetDailyBoxOfficeWithTmdbMatchUseCase,
            weeklyUseCase: GetWeeklyBoxOfficeWithTmdbMatchUseCase,
            nowMillis: Long
        ): BoxOfficeWidgetSnapshot {
            val boxOfficeMovies = when (period) {
                BoxOfficePeriod.DAILY -> dailyUseCase(targetDate = null)
                BoxOfficePeriod.WEEKLY -> weeklyUseCase(targetDate = null)
            }
            return boxOfficeMovies.toWidgetSnapshot(nowMillis)
        }
    }
}
