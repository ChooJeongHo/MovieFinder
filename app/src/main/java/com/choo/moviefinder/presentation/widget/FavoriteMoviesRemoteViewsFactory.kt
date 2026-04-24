package com.choo.moviefinder.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.room.Room
import com.choo.moviefinder.R
import com.choo.moviefinder.data.local.MovieDatabase
import com.choo.moviefinder.data.local.entity.FavoriteMovieEntity
import com.choo.moviefinder.data.local.entity.WatchlistEntity
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.Locale

class FavoriteMoviesRemoteViewsFactory(
    private val context: Context,
    private val appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
) : RemoteViewsService.RemoteViewsFactory {

    // 현재 표시 중인 영화 목록 (즐겨찾기 또는 워치리스트)
    private data class MovieItem(val id: Int, val title: String, val voteAverage: Double)

    private val movies = mutableListOf<MovieItem>()
    private var loadFailed = false

    private val database get() = Companion.getDatabase(context.applicationContext)

    // 초기 생성 시 호출 (데이터는 onDataSetChanged에서 로드)
    override fun onCreate() {
        // Initial data will be loaded in onDataSetChanged
    }

    // SharedPreferences에서 위젯 타입을 읽어 즐겨찾기 또는 워치리스트를 동기 호출로 가져옴
    override fun onDataSetChanged() {
        val widgetType = FavoriteMoviesWidgetConfigureActivity.loadWidgetType(context, appWidgetId)
        try {
            val isWatchlist =
                widgetType == FavoriteMoviesWidgetConfigureActivity.WIDGET_TYPE_WATCHLIST
            val items: List<MovieItem> = if (isWatchlist) {
                runBlocking {
                    database.watchlistDao().getAllWatchlistOnce()
                }.map { entity: WatchlistEntity ->
                    MovieItem(entity.id, entity.title, entity.voteAverage)
                }
            } else {
                runBlocking {
                    database.favoriteMovieDao().getAllFavoritesOnce()
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

    // 위젯 제거 시 영화 데이터 정리
    override fun onDestroy() {
        movies.clear()
    }

    // 위젯에 표시할 영화 개수 반환 (에러 시 에러 행 1개 표시)
    override fun getCount(): Int = if (loadFailed) 1 else movies.size

    // 지정 위치의 영화 RemoteViews 생성 (제목, 평점, 딥링크 클릭)
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
            views.setTextViewText(
                R.id.movie_rating,
                String.format(Locale.US, "★ %.1f", movie.voteAverage)
            )

            // Fill-in intent for item click (deeplink to movie detail)
            val fillInIntent = Intent().apply {
                data = Uri.parse("moviefinder://movie/${movie.id}")
            }
            views.setOnClickFillInIntent(R.id.movie_title, fillInIntent)
            views.setOnClickFillInIntent(R.id.movie_rating, fillInIntent)
        }

        return views
    }

    // 데이터 로딩 중 표시할 임시 뷰 반환
    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_movie_item).apply {
            setTextViewText(R.id.movie_title, context.getString(R.string.widget_loading))
            setTextViewText(R.id.movie_rating, "")
        }
    }

    // 뷰 타입 수 반환 (단일 타입)
    override fun getViewTypeCount(): Int = 1

    // 영화 ID를 안정적인 아이템 ID로 반환
    override fun getItemId(position: Int): Long {
        return if (position < movies.size) movies[position].id.toLong() else position.toLong()
    }

    // 안정적 ID 사용 여부 (영화 ID 기반)
    override fun hasStableIds(): Boolean = true

    companion object {
        @Volatile private var instance: MovieDatabase? = null

        fun releaseDatabase() {
            instance?.close()
            instance = null
        }

        fun getDatabase(context: Context): MovieDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context,
                    MovieDatabase::class.java,
                    "movie_finder_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
