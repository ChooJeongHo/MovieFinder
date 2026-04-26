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
import okhttp3.Cache
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

class PopularMoviesRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private val movies = mutableListOf<WidgetMovie>()
    private var loadFailed = false

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client get() = Companion.getClient(context.applicationContext)

    // 초기 생성 시 호출 (데이터는 onDataSetChanged에서 로드)
    override fun onCreate() {
        // Initial data will be loaded in onDataSetChanged
    }

    // TMDB API에서 인기 영화 Top 10을 동기 호출로 가져옴
    override fun onDataSetChanged() {
        try {
            val url = "https://api.themoviedb.org/3/movie/popular?language=ko-KR&page=1"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.code == 429) {
                    // Rate limited — keep existing data, don't set loadFailed
                    return
                }
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val movieResponse = json.decodeFromString<WidgetMovieListResponse>(body)
                    movies.clear()
                    movies.addAll(movieResponse.results.take(MAX_MOVIES))
                    loadFailed = false
                } else {
                    movies.clear()
                    loadFailed = true
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "위젯: 인기 영화 가져오기 실패")
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
            val ratingText = String.format(Locale.US, "★ %.1f", movie.voteAverage)
            views.setTextViewText(R.id.movie_rating, ratingText)
            views.setContentDescription(
                R.id.movie_rating,
                context.getString(R.string.cd_widget_rating, movie.voteAverage)
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

        @Volatile private var instance: OkHttpClient? = null

        fun releaseClient() {
            synchronized(this) {
                instance = null
            }
        }

        fun getClient(context: Context): OkHttpClient =
            instance ?: synchronized(this) {
                instance ?: OkHttpClient.Builder()
                    // API 키를 Interceptor로 주입 — URL 문자열에 직접 포함 시 예외 로그에 노출될 수 있음
                    .addInterceptor { chain ->
                        val original = chain.request()
                        val url = original.url.newBuilder()
                            .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
                            .build()
                        chain.proceed(original.newBuilder().url(url).build())
                    }
                    .cache(Cache(File(context.cacheDir, "widget_http_cache"), 5L * 1024 * 1024))
                    .apply {
                        // 디버그 빌드(에뮬레이터)에서는 인증서 피닝 비활성화
                        if (!BuildConfig.DEBUG) {
                            certificatePinner(
                                CertificatePinner.Builder()
                                    .add(
                                        "api.themoviedb.org",
                                        "sha256/f78NVAesYtdZ9OGSbK7VtGQkSIVykh3DnduuLIJHMu4=",
                                        "sha256/G9LNNAql897egYsabashkzUCTEJkWBzgoEtk8X/678c="
                                    )
                                    .build()
                            )
                        }
                    }
                    .connectTimeout(15.seconds)
                    .readTimeout(15.seconds)
                    .build()
                    .also { instance = it }
            }
    }
}

@Serializable
internal data class WidgetMovieListResponse(
    @SerialName("results") val results: List<WidgetMovie>
)

@Serializable
internal data class WidgetMovie(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String = "",
    @SerialName("vote_average") val voteAverage: Double = 0.0
)
