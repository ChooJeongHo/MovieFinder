package com.choo.moviefinder.core.startup

import android.content.Context
import android.os.StrictMode
import androidx.startup.Initializer

/**
 * 디버그 빌드 전용 StrictMode 설정.
 * 메인 스레드에서의 디스크/네트워크 접근과 리소스 누수를 감지하여 Logcat에 기록한다.
 * penaltyLog()만 사용하므로 앱이 크래시되지 않는다.
 */
class StrictModeInitializer : Initializer<Unit> {
    // 메인 스레드 정책과 VM 정책을 설정한다
    override fun create(context: Context) {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .build()
        )
    }

    // TimberInitializer에 의존하여 Timber 초기화 후 실행되도록 한다
    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(TimberInitializer::class.java)
}
