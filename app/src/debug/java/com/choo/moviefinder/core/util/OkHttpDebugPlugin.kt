package com.choo.moviefinder.core.util

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

fun OkHttpClient.Builder.addDebugLogging(): OkHttpClient.Builder = addInterceptor(
    HttpLoggingInterceptor { Timber.tag("OkHttp").d(it) }
        .apply { level = HttpLoggingInterceptor.Level.HEADERS }
)
