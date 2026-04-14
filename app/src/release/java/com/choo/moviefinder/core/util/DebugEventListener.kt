package com.choo.moviefinder.core.util

import okhttp3.EventListener

// 릴리스 빌드용 no-op: 로깅 코드가 릴리스 바이너리에 포함되지 않도록 소스셋 분리
class DebugEventListener : EventListener()
