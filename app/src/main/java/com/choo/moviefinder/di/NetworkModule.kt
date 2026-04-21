package com.choo.moviefinder.di

import android.content.Context
import com.choo.moviefinder.BuildConfig
import com.choo.moviefinder.core.util.DebugEventListener
import com.choo.moviefinder.core.util.NetworkMonitor
import com.choo.moviefinder.core.util.addDebugLogging
import com.choo.moviefinder.data.remote.api.MovieApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.CertificatePinner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import java.io.File
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Qualifier
import kotlin.time.Duration.Companion.seconds
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ImageOkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // kotlinx.serialization JSON 파서를 설정하여 제공한다
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        // isLenient 제거: TMDB 스펙 명확, 파싱 오류를 즉시 노출시켜야 함
    }

    // API 통신용 OkHttpClient를 인증서 피닝과 API 키 인터셉터를 포함하여 제공한다
    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val cache = Cache(File(context.cacheDir, "http_cache"), HTTP_CACHE_SIZE)

        return OkHttpClient.Builder()
            .cache(cache)
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
                if (BuildConfig.DEBUG) eventListener(DebugEventListener())
            }
            .addDebugLogging()
            // User-Agent: TMDB 측 로그 식별 및 API 정책 준수
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "MovieFinder/${BuildConfig.VERSION_NAME} (Android)")
                        .build()
                )
            }
            // API 키 주입
            .addInterceptor { chain ->
                val original = chain.request()
                val url = original.url.newBuilder()
                    .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
                    .build()
                chain.proceed(original.newBuilder().url(url).build())
            }
            // 429 Rate-Limit: Retry-After 헤더를 존중하여 1회 재시도 (최대 5초 대기)
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                if (response.code == 429) {
                    val retryAfterMs = (response.header("Retry-After")?.toLongOrNull() ?: 1L)
                        .coerceAtMost(5L) * 1000L
                    response.close()
                    Thread.sleep(retryAfterMs)
                    chain.proceed(chain.request())
                } else {
                    response
                }
            }
            // 엔드포인트별 캐시 TTL 오버라이드 (서버 Cache-Control 무시하는 경우 대비)
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                val path = chain.request().url.encodedPath
                val cc = cacheTtlHeader(path)
                response.newBuilder().header("Cache-Control", cc).build()
            }
            // 타임아웃 15초: ExponentialBackoff 재시도(최대 3회)와 조합하면 충분
            .connectTimeout(15.seconds)
            .readTimeout(15.seconds)
            .writeTimeout(15.seconds)
            .build()
    }

    // TMDB API 베이스 URL로 Retrofit 인스턴스를 생성하여 제공한다
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.TMDB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    // Retrofit으로 MovieApiService 구현체를 생성하여 제공한다
    @Provides
    @Singleton
    fun provideMovieApiService(retrofit: Retrofit): MovieApiService {
        return retrofit.create(MovieApiService::class.java)
    }

    // 이미지 로딩용 OkHttpClient를 image.tmdb.org 인증서 피닝과 함께 제공한다
    @Provides
    @Singleton
    @ImageOkHttpClient
    fun provideImageOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .apply {
                if (!BuildConfig.DEBUG) {
                    certificatePinner(
                        CertificatePinner.Builder()
                            .add(
                                "image.tmdb.org",
                                "sha256/B/uFV1xlBj83gXfsZfdC7IUKMYq9E0EgYKJaTVUAbus=",
                                "sha256/kZwN96eHtZftBWrOZUsd6cA4es80n3NzSk/XtYz2EqQ="
                            )
                            .build()
                    )
                }
                if (BuildConfig.DEBUG) eventListener(DebugEventListener())
            }
            .connectTimeout(15.seconds)
            .readTimeout(15.seconds)
            .writeTimeout(15.seconds)
            .build()
    }

    // 실시간 네트워크 연결 상태 모니터를 제공한다
    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitor(context)
    }

    // 엔드포인트 경로에 따른 Cache-Control 헤더 값 반환
    private fun cacheTtlHeader(path: String): String {
        val ttl = when {
            path.contains("/genre/movie/list") -> 86400 // 장르 목록: 1일
            path.contains("/trending") -> 60 // 트렌딩: 1분
            path.contains("/search") -> 0 // 검색: 캐시 안 함
            else -> 300 // 그 외: 5분
        }
        return if (ttl > 0) "public, max-age=$ttl" else "no-store"
    }

    private const val HTTP_CACHE_SIZE = 10L * 1024 * 1024 // 10MB
}
