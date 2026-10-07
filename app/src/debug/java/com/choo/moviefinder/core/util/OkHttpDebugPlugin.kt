package com.choo.moviefinder.core.util

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

fun OkHttpClient.Builder.addDebugLogging(): OkHttpClient.Builder = addInterceptor(
    HttpLoggingInterceptor { Timber.tag("OkHttp").d(it) }
        .apply {
            level = HttpLoggingInterceptor.Level.HEADERS
            redactHeader("Authorization")
            // 인터셉터 등록 순서와 무관하게 요청/응답/실패 줄의 URL에서 값을 가린다
            // (응답 줄은 키가 주입된 최종 요청 URL을 찍으므로 로깅을 키 주입 앞에 둬도 노출된다)
            redactQueryParams(*SecretQueryParams.ALL.toTypedArray())
        }
)
