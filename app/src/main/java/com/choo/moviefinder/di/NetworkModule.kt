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
import okhttp3.CertificatePinner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ImageOkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val certificatePinner = CertificatePinner.Builder()
            .add(
                "api.themoviedb.org",
                "sha256/f78NVAesYtdZ9OGSbK7VtGQkSIVykh3DnduuLIJHMu4=",
                "sha256/G9LNNAql897egYsabashkzUCTEJkWBzgoEtk8X/678c="
            )
            .build()

        return OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .addInterceptor { chain ->
                val original = chain.request()
                val url = original.url.newBuilder()
                    .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
                    .build()
                chain.proceed(original.newBuilder().url(url).build())
            }
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
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.TMDB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideMovieApiService(retrofit: Retrofit): MovieApiService {
        return retrofit.create(MovieApiService::class.java)
    }

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
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitor(context)
    }
}