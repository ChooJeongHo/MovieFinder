@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.choo.moviefinder.core.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.time.Clock
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager by lazy { WorkManager.getInstance(context) }

    // 워치리스트 영화 알림을 지정된 시각에 예약한다 (delay ≤ 0이면 예약하지 않는다)
    fun schedule(movieId: Int, movieTitle: String, dateMillis: Long) {
        val delay = dateMillis - Clock.System.now().toEpochMilliseconds()
        if (delay <= 0) {
            Timber.d("알림 시각이 과거이므로 영화 %d 워치리스트 알림 건너뜀", movieId)
            return
        }
        val request = OneTimeWorkRequestBuilder<WatchlistReminderWorker>()
            .setInputData(
                workDataOf(
                    WatchlistReminderWorker.KEY_MOVIE_ID to movieId,
                    WatchlistReminderWorker.KEY_MOVIE_TITLE to movieTitle
                )
            )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("watchlist_reminder")
            .build()
        workManager.enqueueUniqueWork(
            "watchlist_reminder_$movieId",
            ExistingWorkPolicy.REPLACE,
            request
        )
        Timber.d("영화 %d (%s) 워치리스트 알림 예약됨", movieId, movieTitle)
    }

    // 예약된 워치리스트 영화 알림을 취소한다
    fun cancel(movieId: Int) {
        workManager.cancelUniqueWork("watchlist_reminder_$movieId")
        Timber.d("영화 %d 워치리스트 알림 취소됨", movieId)
    }
}
