package com.choo.moviefinder.core.util

import okhttp3.OkHttpClient

// 릴리스 빌드용 no-op: 로깅 코드가 릴리스 바이너리에 포함되지 않도록 소스셋 분리
fun OkHttpClient.Builder.addDebugLogging(): OkHttpClient.Builder = this
