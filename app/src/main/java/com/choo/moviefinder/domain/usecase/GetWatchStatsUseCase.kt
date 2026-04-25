@file:OptIn(
    kotlin.time.ExperimentalTime::class,
    kotlinx.coroutines.ExperimentalCoroutinesApi::class,
    kotlinx.coroutines.FlowPreview::class
)

package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.core.util.currentMonthStartMillis
import com.choo.moviefinder.domain.model.GenreCount
import com.choo.moviefinder.domain.model.MonthlyWatchCount
import com.choo.moviefinder.domain.model.WatchStats
import com.choo.moviefinder.domain.repository.PreferencesRepository
import com.choo.moviefinder.domain.repository.UserRatingRepository
import com.choo.moviefinder.domain.repository.WatchHistoryRepository
import dagger.Reusable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@Reusable
class GetWatchStatsUseCase @Inject constructor(
    private val watchHistoryRepository: WatchHistoryRepository,
    private val userRatingRepository: UserRatingRepository,
    private val preferencesRepository: PreferencesRepository
) {
    // 시청 통계 데이터를 여러 Flow를 combine하여 반환한다
    // 1시간마다 tickerFlow가 재방출하여 monthStartMillis·threeMonthsAgoMillis를 새로 계산한다 (날짜 경계 넘김 대응)
    operator fun invoke(): Flow<WatchStats> = tickerFlow(ONE_HOUR_MILLIS)
        .flatMapLatest {
            val monthStartMillis = currentMonthStartMillis()
            val threeMonthsAgoMillis = kotlin.time.Clock.System.now().toEpochMilliseconds() - THREE_MONTHS_MILLIS

            val genreCountsFlow = watchHistoryRepository.getGenreCounts()
                .distinctUntilChanged()

            val baseStatsFlow = combine(
                watchHistoryRepository.getTotalWatchedCount().distinctUntilChanged(),
                watchHistoryRepository.getWatchedCountSince(monthStartMillis).distinctUntilChanged(),
                userRatingRepository.getAverageUserRating().distinctUntilChanged(),
                genreCountsFlow,
                watchHistoryRepository.getMonthlyWatchCounts().distinctUntilChanged()
            ) { total, monthly, avgRating, genreCounts, monthlyCounts ->
                BaseStats(total, monthly, avgRating, genreCounts, monthlyCounts)
            }

            combine(
                baseStatsFlow,
                preferencesRepository.getMonthlyWatchGoal().distinctUntilChanged(),
                userRatingRepository.getRatingDistribution().distinctUntilChanged(),
                watchHistoryRepository.getDailyWatchCounts(threeMonthsAgoMillis).distinctUntilChanged()
            ) { base, watchGoal, ratingDist, dailyCounts ->
                WatchStats(
                    totalWatched = base.total,
                    monthlyWatched = base.monthly,
                    averageRating = base.avgRating,
                    topGenres = base.genreCounts.take(3),
                    allGenreCounts = base.genreCounts,
                    monthlyWatchCounts = base.monthlyCounts,
                    monthlyWatchGoal = watchGoal,
                    ratingDistribution = ratingDist,
                    dailyWatchCounts = dailyCounts
                )
            }.debounce(100L)
        }
        .flowOn(Dispatchers.Default)

    // 즉시 첫 번째 틱을 방출한 후 intervalMillis마다 반복 방출한다
    private fun tickerFlow(intervalMillis: Long): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(intervalMillis)
        }
    }

    // 5개 기본 통계 Flow를 묶기 위한 내부 데이터 홀더
    private data class BaseStats(
        val total: Int,
        val monthly: Int,
        val avgRating: Float?,
        val genreCounts: List<GenreCount>,
        val monthlyCounts: List<MonthlyWatchCount>
    )

    companion object {
        // 캘린더 히트맵 표시 범위와 일치하는 3개월(약 90일) 밀리초
        private const val THREE_MONTHS_MILLIS = 90L * 24 * 60 * 60 * 1000

        // 날짜 경계 넘김 대응: 1시간마다 타임스탬프를 재계산하여 통계 범위를 갱신한다
        private const val ONE_HOUR_MILLIS = 60L * 60 * 1000
    }
}
