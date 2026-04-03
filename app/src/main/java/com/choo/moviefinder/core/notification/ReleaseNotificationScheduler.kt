package com.choo.moviefinder.core.notification

import android.content.Context
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReleaseNotificationScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    // 영화 개봉일에 맞춰 알림을 WorkManager로 예약한다
    fun schedule(movieId: Int, movieTitle: String, releaseDate: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Timber.d("Skipping notification scheduling on API < 26")
            return
        }

        val releaseDateMillis = parseReleaseDate(releaseDate) ?: run {
            Timber.w("Failed to parse release date: %s for movie %d", releaseDate, movieId)
            return
        }

        val now = Clock.System.now().toEpochMilliseconds()
        val delay = releaseDateMillis - now

        if (delay <= 0) {
            Timber.d("Release date is today or in the past, skipping notification for movie %d", movieId)
            return
        }

        val workRequest = OneTimeWorkRequestBuilder<ReleaseNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ReleaseNotificationWorker.KEY_MOVIE_ID to movieId,
                    ReleaseNotificationWorker.KEY_MOVIE_TITLE to movieTitle
                )
            )
            .addTag("release_$movieId")
            .build()

        val workName = "release_$movieId"
        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, workRequest)

        Timber.d("Scheduled release notification for movie %d (%s) at %s", movieId, movieTitle, releaseDate)
    }

    // 예약된 개봉일 알림을 취소한다
    fun cancel(movieId: Int) {
        val workName = "release_$movieId"
        WorkManager.getInstance(context).cancelUniqueWork(workName)
        Timber.d("Cancelled release notification for movie %d", movieId)
    }

    // 개봉일 문자열을 당일 오전 9시 기준 밀리초 타임스탬프로 변환한다
    private fun parseReleaseDate(releaseDate: String): Long? {
        return try {
            val date = LocalDate.parse(releaseDate)
            val notificationTime = LocalDateTime(date.year, date.month, date.dayOfMonth, 9, 0, 0)
            notificationTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        } catch (e: Exception) {
            null
        }
    }
}
