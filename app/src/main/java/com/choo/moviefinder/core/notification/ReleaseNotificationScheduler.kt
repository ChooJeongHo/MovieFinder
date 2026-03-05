package com.choo.moviefinder.core.notification

import android.content.Context
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReleaseNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun schedule(movieId: Int, movieTitle: String, releaseDate: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Timber.d("Skipping notification scheduling on API < 26")
            return
        }

        val releaseDateMillis = parseReleaseDate(releaseDate) ?: run {
            Timber.w("Failed to parse release date: %s for movie %d", releaseDate, movieId)
            return
        }

        val notificationTime = Calendar.getInstance().apply {
            timeInMillis = releaseDateMillis
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()
        val delay = notificationTime.timeInMillis - now

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

    fun cancel(movieId: Int) {
        val workName = "release_$movieId"
        WorkManager.getInstance(context).cancelUniqueWork(workName)
        Timber.d("Cancelled release notification for movie %d", movieId)
    }

    private fun parseReleaseDate(releaseDate: String): Long? {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            format.parse(releaseDate)?.time
        } catch (e: Exception) {
            null
        }
    }
}
