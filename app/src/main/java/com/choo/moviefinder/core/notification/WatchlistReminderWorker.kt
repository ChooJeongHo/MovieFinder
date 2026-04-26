package com.choo.moviefinder.core.notification

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.choo.moviefinder.MainActivity
import com.choo.moviefinder.R
import timber.log.Timber

class WatchlistReminderWorker(
    context: android.content.Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val movieId = inputData.getInt(KEY_MOVIE_ID, -1)
        val movieTitle = inputData.getString(KEY_MOVIE_TITLE) ?: return Result.failure()
        if (movieId == -1) return Result.failure()

        // Android 13+에서 POST_NOTIFICATIONS 권한 미부여 시 크래시 방지
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                applicationContext, android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                Timber.w("POST_NOTIFICATIONS 권한 미부여, 워치리스트 알림 건너뜀")
                return Result.success()
            }
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("moviefinder://movie/$movieId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, movieId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            applicationContext, WATCHLIST_REMINDER_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_favorite)
            .setContentTitle(applicationContext.getString(R.string.reminder_notification_title))
            .setContentText(applicationContext.getString(R.string.reminder_notification_body, movieTitle))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(movieId, notification)

        return Result.success()
    }

    companion object {
        const val KEY_MOVIE_ID = "movie_id"
        const val KEY_MOVIE_TITLE = "movie_title"
        const val WATCHLIST_REMINDER_CHANNEL_ID = "watchlist_reminder_channel"
    }
}
