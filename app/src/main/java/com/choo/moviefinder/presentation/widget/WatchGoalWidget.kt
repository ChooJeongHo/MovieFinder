package com.choo.moviefinder.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.choo.moviefinder.R
import com.choo.moviefinder.core.util.currentMonthStartMillis
import com.choo.moviefinder.data.local.dao.WatchHistoryDao
import com.choo.moviefinder.domain.repository.PreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class WatchGoalWidget : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WatchGoalWidgetEntryPoint {
        fun watchHistoryDao(): WatchHistoryDao
        fun preferencesRepository(): PreferencesRepository
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WatchGoalWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.choo.moviefinder.widget.ACTION_GOAL_REFRESH"

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val (watched, goal) = loadData(context)

            val progressText = context.getString(R.string.widget_goal_progress, watched, goal)
            val progressValue = if (goal > 0) ((watched.toFloat() / goal) * 100).toInt().coerceAtMost(100) else 0

            val views = RemoteViews(context.packageName, R.layout.widget_watch_goal).apply {
                setTextViewText(R.id.tv_goal_progress, progressText)
                setProgressBar(R.id.progress_goal, 100, progressValue, false)
            }

            val refreshIntent = Intent(context, WatchGoalWidget::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_goal_refresh, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun loadData(context: Context): Pair<Int, Int> {
            return try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    WatchGoalWidgetEntryPoint::class.java
                )
                runBlocking {
                    val watched = entryPoint.watchHistoryDao()
                        .getCountSince(currentMonthStartMillis())
                        .first()
                    val goal = entryPoint.preferencesRepository()
                        .getMonthlyWatchGoal()
                        .first()
                    Pair(watched, goal)
                }
            } catch (e: Exception) {
                Timber.w(e, "위젯: 시청 목표 데이터 로드 실패")
                Pair(0, 0)
            }
        }
    }
}
