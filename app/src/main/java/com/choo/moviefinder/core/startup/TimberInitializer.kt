package com.choo.moviefinder.core.startup

import android.content.Context
import androidx.startup.Initializer
import com.choo.moviefinder.BuildConfig
import timber.log.Timber

class TimberInitializer : Initializer<Unit> {
    // 디버그 빌드에서 Timber DebugTree를 심어 로깅을 활성화한다
    override fun create(context: Context) {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    // 의존하는 Initializer가 없으므로 빈 리스트를 반환한다
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}