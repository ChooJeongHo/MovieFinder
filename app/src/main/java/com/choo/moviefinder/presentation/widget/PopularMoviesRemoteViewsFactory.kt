package com.choo.moviefinder.presentation.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.choo.moviefinder.BuildConfig
import com.choo.moviefinder.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.TimeUnit

class PopularMoviesRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private val movies = mutableListOf<WidgetMovie>()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun onCreate() {
        // Initial data will be loaded in onDataSetChanged
    }

    override fun onDataSetChanged() {
        try {
            val url = "https://api.themoviedb.org/3/movie/popular" +
                "?api_key=${BuildConfig.TMDB_API_KEY}&language=ko-KR&page=1"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (response.isSuccessful && body != null) {
                val movieResponse = json.decodeFromString<WidgetMovieListResponse>(body)
                movies.clear()
                movies.addAll(movieResponse.results.take(MAX_MOVIES))
            }
        } catch (e: Exception) {
            Timber.w(e, "Widget: Failed to fetch popular movies")
        }
    }

    override fun onDestroy() {
        movies.clear()
    }

    override fun getCount(): Int = movies.size

    override fun getViewAt(position: Int): RemoteViews {
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

    companion object {
        private const val MAX_MOVIES = 10
    }
}

@Serializable
private data class WidgetMovieListResponse(
    @SerialName("results") val results: List<WidgetMovie>
)

@Serializable
private data class WidgetMovie(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String = "",
    @SerialName("vote_average") val voteAverage: Double = 0.0
)
