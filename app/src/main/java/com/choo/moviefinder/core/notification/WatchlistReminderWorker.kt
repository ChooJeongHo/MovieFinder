package com.choo.moviefinder.core.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.choo.moviefinder.MainActivity
import com.choo.moviefinder.R

class WatchlistReminderWorker(
    context: android.content.Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val movieId = inputData.getInt(KEY_MOVIE_ID, -1)
        val movieTitle = inputData.getString(KEY_MOVIE_TITLE) ?: return Result.failure()
        if (movieId == -1) return Result.failure()

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

        val notificationManager = ContextCompat.getSystemService(
            applicationContext, NotificationManager::class.java
        ) as NotificationManager
        notificationManager.notify(movieId, notification)

        return Result.success()
    }

    companion object {
        const val KEY_MOVIE_ID = "movie_id"
        const val KEY_MOVIE_TITLE = "movie_title"
        const val WATCHLIST_REMINDER_CHANNEL_ID = "watchlist_reminder_channel"
    }
}
