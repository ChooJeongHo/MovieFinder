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
import com.choo.moviefinder.MovieFinderApp
import com.choo.moviefinder.R
import com.choo.moviefinder.core.util.currentMonthStartMillis
import com.choo.moviefinder.core.util.currentYearMonth
import com.choo.moviefinder.domain.repository.PreferencesRepository
import com.choo.moviefinder.domain.repository.WatchHistoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchGoalNotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val watchHistoryRepository: WatchHistoryRepository
) {

    private val checkMutex = Mutex()

    // 시청 목표 달성 여부를 확인하고 알림을 표시한다 (Mutex로 중복 알림 방지)
    suspend fun checkAndNotifyGoalAchieved() = checkMutex.withLock {
        val goal = preferencesRepository.getMonthlyWatchGoal().first()
        if (goal <= 0) return

        val monthStartMillis = currentMonthStartMillis()
        val currentCount = watchHistoryRepository.getWatchedCountSince(monthStartMillis).first()
        if (currentCount < goal) return

        val yearMonth = currentYearMonth()
        val lastNotified = preferencesRepository.getLastGoalNotifiedMonth().first()
        if (lastNotified == yearMonth) return

        preferencesRepository.setLastGoalNotifiedMonth(yearMonth)
        showGoalAchievedNotification()
    }

    // 목표 달성 축하 알림을 표시한다 (테스트에서 오버라이드 가능)
    internal fun showGoalAchievedNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                Timber.w("POST_NOTIFICATIONS 권한 미부여, 목표 알림 건너뜀")
                return
            }
        }

        val deepLinkUri = Uri.parse("moviefinder://stats")
        val intent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            GOAL_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MovieFinderApp.WATCH_GOAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_movie)
            .setContentTitle(context.getString(R.string.notification_goal_achieved_title))
            .setContentText(context.getString(R.string.notification_goal_achieved_text))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(GOAL_NOTIFICATION_ID, notification)
        Timber.d("시청 목표 달성 알림 표시됨")
    }

    companion object {
        private const val GOAL_NOTIFICATION_ID = 9999
    }
}
