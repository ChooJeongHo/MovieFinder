package com.choo.moviefinder.core.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.choo.moviefinder.MovieFinderApp
import com.choo.moviefinder.R
import timber.log.Timber

class ReleaseNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    // 개봉일 알림을 생성하여 사용자에게 표시한다
    override fun doWork(): Result {
        val movieId = inputData.getInt(KEY_MOVIE_ID, -1)
        val movieTitle = inputData.getString(KEY_MOVIE_TITLE) ?: return Result.failure()

        if (movieId == -1) return Result.failure()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                Timber.w("POST_NOTIFICATIONS permission not granted, skipping notification for movie %d", movieId)
                return Result.success()
            }
        }

        val deepLinkUri = Uri.parse("moviefinder://movie/$movieId")
        val intent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            movieId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            MovieFinderApp.RELEASE_DATE_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_movie)
            .setContentTitle(movieTitle)
            .setContentText(applicationContext.getString(R.string.notification_release_today))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(movieId, notification)

        return Result.success()
    }

    companion object {
        const val KEY_MOVIE_ID = "movie_id"
        const val KEY_MOVIE_TITLE = "movie_title"
    }
}
