package com.choo.moviefinder.di

import android.content.Context
import com.choo.moviefinder.BuildConfig
import com.choo.moviefinder.core.util.NetworkMonitor
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
import okhttp3.logging.HttpLoggingInterceptor
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
        isLenient = true
    }

    // API 통신용 OkHttpClient를 인증서 피닝과 API 키 인터셉터를 포함하여 제공한다
    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val cache = Cache(File(context.cacheDir, "http_cache"), HTTP_CACHE_SIZE)
        val certificatePinner = CertificatePinner.Builder()
            .add(
                "api.themoviedb.org",
                "sha256/f78NVAesYtdZ9OGSbK7VtGQkSIVykh3DnduuLIJHMu4=",
                "sha256/G9LNNAql897egYsabashkzUCTEJkWBzgoEtk8X/678c="
            )
            .build()

        return OkHttpClient.Builder()
            .cache(cache)
            .certificatePinner(certificatePinner)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor { message ->
                            timber.log.Timber.tag("OkHttp").d(message)
                        }.apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                }
            }
            .addInterceptor { chain ->
                val original = chain.request()
                val url = original.url.newBuilder()
                    .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
                    .build()
                chain.proceed(original.newBuilder().url(url).build())
            }
            .connectTimeout(30.seconds)
            .readTimeout(30.seconds)
            .writeTimeout(30.seconds)
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
        val certificatePinner = CertificatePinner.Builder()
            .add(
                "image.tmdb.org",
                "sha256/lTfe0l8BsNLrIpmBOrNBdSg6TZn+VIEQ81pHilsTbqA=",
                "sha256/AlSQhgtJirc8ahLyekmtX+Iw+v46yPYRLJt9Cq1GlB0="
            )
            .build()

        return OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .connectTimeout(30.seconds)
            .readTimeout(30.seconds)
            .writeTimeout(30.seconds)
            .build()
    }

    // 실시간 네트워크 연결 상태 모니터를 제공한다
    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitor(context)
    }

    private const val HTTP_CACHE_SIZE = 10L * 1024 * 1024 // 10MB
}