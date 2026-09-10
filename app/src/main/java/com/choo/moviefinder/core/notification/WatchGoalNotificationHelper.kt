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
import com.choo.moviefinder.MainActivity
import com.choo.moviefinder.MovieFinderApp
import com.choo.moviefinder.R
import com.choo.moviefinder.domain.usecase.CheckWatchGoalAchievedUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchGoalNotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val checkWatchGoalAchievedUseCase: CheckWatchGoalAchievedUseCase
) {

    private val checkMutex = Mutex()

    // 시청 목표 달성 여부를 확인하고 알림을 표시한다 (Mutex로 중복 알림 방지)
    suspend fun checkAndNotifyGoalAchieved() = checkMutex.withLock {
        if (checkWatchGoalAchievedUseCase()) {
            showGoalAchievedNotification()
        }
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

        val pendingIntent = PendingIntent.getActivity(
            context,
            GOAL_NOTIFICATION_ID,
            buildStatsDeepLinkIntent(),
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

    // nav_graph.xml의 statsFragment deepLink(moviefinder://stats)는 path 세그먼트가 없어,
    // Navigation Safe Args가 생성하는 매니페스트 intent-filter는 <data android:path="/" />를
    // 요구하는데 실제 Uri의 path는 빈 문자열이라 일치하지 않는다. movie/person 딥링크는
    // pathPrefix="/"라 이 문제가 없고, search/favorite 정적 단축키는 shortcuts.xml에
    // targetClass가 있어 PackageManager 매칭 자체를 우회한다 — stats 알림만
    // implicit ACTION_VIEW로 만들어 실기기에서 "Activity not started"로 실패했다.
    // shortcuts.xml과 동일하게 MainActivity를 explicit component로 지정해 PackageManager의
    // intent-filter data 매칭을 우회한다 (NavController.handleDeepLink는 컴포넌트 지정 여부와
    // 무관하게 intent.data를 그대로 nav_graph deepLink 패턴과 매칭하므로 화면 이동은 그대로 동작).
    // 테스트에서 NotificationCompat.Builder 전체 경로를 건드리지 않고 검증할 수 있도록 별도 함수로 분리.
    internal fun buildStatsDeepLinkIntent(): Intent {
        val deepLinkUri = Uri.parse("moviefinder://stats")
        return Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
            setClass(context, MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    companion object {
        private const val GOAL_NOTIFICATION_ID = 9999
    }
}
