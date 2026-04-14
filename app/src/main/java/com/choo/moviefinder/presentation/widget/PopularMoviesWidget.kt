package com.choo.moviefinder.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.choo.moviefinder.R

class PopularMoviesWidget : AppWidgetProvider() {

    // 위젯 갱신 시 각 위젯 인스턴스에 RemoteViewsService 연결
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    // 새로고침 브로드캐스트 수신 시 위젯 데이터 갱신 트리거
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PopularMoviesWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            @Suppress("DEPRECATION")
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_view)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.choo.moviefinder.widget.ACTION_REFRESH"
        const val EXTRA_MOVIE_ID = "com.choo.moviefinder.widget.EXTRA_MOVIE_ID"

        // 위젯 RemoteViews 구성 (ListView 어댑터, 새로고침 버튼, 아이템 클릭 PendingIntent)
        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val serviceIntent = Intent(context, PopularMoviesRemoteViewsService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }

            val views = RemoteViews(context.packageName, R.layout.widget_popular_movies).apply {
                @Suppress("DEPRECATION")
                setRemoteAdapter(R.id.list_view, serviceIntent)
                setEmptyView(R.id.list_view, R.id.empty_view)
            }

            // Refresh button pending intent
            val refreshIntent = Intent(context, PopularMoviesWidget::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)

            // Item click pending intent template (deeplink to movie detail)
            val itemClickIntent = Intent(Intent.ACTION_VIEW).apply {
                setPackage(context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val itemClickPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                itemClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.list_view, itemClickPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
