package com.choo.moviefinder.presentation.widget

import android.content.Intent
import android.widget.RemoteViewsService

class FavoriteMoviesRemoteViewsService : RemoteViewsService() {

    // 위젯 ListView용 RemoteViewsFactory 인스턴스 생성
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return FavoriteMoviesRemoteViewsFactory(applicationContext)
    }
}
