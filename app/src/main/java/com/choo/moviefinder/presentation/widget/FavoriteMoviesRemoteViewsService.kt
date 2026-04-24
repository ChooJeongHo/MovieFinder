package com.choo.moviefinder.presentation.widget

import android.content.Intent
import android.widget.RemoteViewsService

class FavoriteMoviesRemoteViewsService : RemoteViewsService() {

    // 위젯 ListView용 RemoteViewsFactory 인스턴스 생성 (appWidgetId 전달)
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,
            android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
        )
        return FavoriteMoviesRemoteViewsFactory(applicationContext, appWidgetId)
    }
}
