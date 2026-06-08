package com.choo.moviefinder.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.choo.moviefinder.R
import com.choo.moviefinder.data.local.dao.FavoriteMovieDao
import com.choo.moviefinder.data.local.dao.WatchlistDao
import com.choo.moviefinder.data.local.entity.FavoriteMovieEntity
import com.choo.moviefinder.data.local.entity.WatchlistEntity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.Locale

class FavoriteMoviesRemoteViewsFactory(
    private val context: Context,
    private val appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
) : RemoteViewsService.RemoteViewsFactory {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FavoriteWidgetEntryPoint {
        fun favoriteMovieDao(): FavoriteMovieDao
        fun watchlistDao(): WatchlistDao
    }

    private data class MovieItem(val id: Int, val title: String, val voteAverage: Double)

    private val movies = mutableListOf<MovieItem>()
    private var loadFailed = false

    private val entryPoint: FavoriteWidgetEntryPoint
        get() = EntryPointAccessors.fromApplication(
            context.applicationContext,
            FavoriteWidgetEntryPoint::class.java
        )

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val widgetType = FavoriteMoviesWidgetConfigureActivity.loadWidgetType(context, appWidgetId)
        try {
            val isWatchlist =
                widgetType == FavoriteMoviesWidgetConfigureActivity.WIDGET_TYPE_WATCHLIST
            val items: List<MovieItem> = if (isWatchlist) {
                runBlocking {
                    entryPoint.watchlistDao().getAllWatchlistOnce()
                }.map { entity: WatchlistEntity ->
                    MovieItem(entity.id, entity.title, entity.voteAverage)
                }
            } else {
                runBlocking {
                    entryPoint.favoriteMovieDao().getAllFavoritesOnce()
                }.map { entity: FavoriteMovieEntity ->
                    MovieItem(entity.id, entity.title, entity.voteAverage)
                }
            }
            movies.clear()
            movies.addAll(items)
            loadFailed = false
        } catch (e: Exception) {
            Timber.w(e, "위젯: 영화 목록 가져오기 실패 (type=$widgetType)")
            movies.clear()
            loadFailed = true
        }
    }

    override fun onDestroy() {
        movies.clear()
    }

    override fun getCount(): Int = if (loadFailed) 1 else movies.size

    override fun getViewAt(position: Int): RemoteViews {
        if (loadFailed) {
            return RemoteViews(context.packageName, R.layout.widget_movie_item).apply {
                setTextViewText(R.id.movie_title, context.getString(R.string.widget_empty))
                setTextViewText(R.id.movie_rating, "")
            }
        }

        val views = RemoteViews(context.packageName, R.layout.widget_movie_item)

        if (position < movies.size) {
            val movie = movies[position]
            views.setTextViewText(R.id.movie_title, movie.title)
            val ratingText = String.format(Locale.US, "★ %.1f", movie.voteAverage)
            views.setTextViewText(R.id.movie_rating, ratingText)
            views.setContentDescription(
                R.id.movie_rating,
                context.getString(R.string.cd_widget_rating, movie.voteAverage)
            )

            val fillInIntent = Intent().apply {
                data = Uri.parse("moviefinder://movie/${movie.id}")
            }
            views.setOnClickFillInIntent(R.id.movie_title, fillInIntent)
            views.setOnClickFillInIntent(R.id.movie_rating, fillInIntent)
        }

        return views
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_movie_item).apply {
            setTextViewText(R.id.movie_title, context.getString(R.string.widget_loading))
            setTextViewText(R.id.movie_rating, "")
        }
    }

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return if (position < movies.size) movies[position].id.toLong() else position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
