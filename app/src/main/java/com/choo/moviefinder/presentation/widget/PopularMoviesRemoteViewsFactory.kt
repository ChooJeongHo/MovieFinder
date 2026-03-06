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

    // 초기 생성 시 호출 (데이터는 onDataSetChanged에서 로드)
    override fun onCreate() {
        // Initial data will be loaded in onDataSetChanged
    }

    // TMDB API에서 인기 영화 Top 10을 동기 호출로 가져옴
    override fun onDataSetChanged() {
        try {
            val url = "https://api.themoviedb.org/3/movie/popular" +
                "?api_key=${BuildConfig.TMDB_API_KEY}&language=ko-KR&page=1"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val movieResponse = json.decodeFromString<WidgetMovieListResponse>(body)
                    movies.clear()
                    movies.addAll(movieResponse.results.take(MAX_MOVIES))
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Widget: Failed to fetch popular movies")
        }
    }

    // 위젯 제거 시 영화 데이터 정리
    override fun onDestroy() {
        movies.clear()
    }

    // 위젯에 표시할 영화 개수 반환
    override fun getCount(): Int = movies.size

    // 지정 위치의 영화 RemoteViews 생성 (제목, 평점, 딥링크 클릭)
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
