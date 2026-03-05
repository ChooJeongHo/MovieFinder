package com.choo.moviefinder.presentation.widget

import android.content.Intent
import android.widget.RemoteViewsService

class PopularMoviesRemoteViewsService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return PopularMoviesRemoteViewsFactory(applicationContext)
    }
}
