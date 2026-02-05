package com.choo.moviefinder.core.startup

import android.content.Context
import android.os.StrictMode
import androidx.startup.Initializer
import com.choo.moviefinder.BuildConfig

/**
 * 디버그 빌드 전용 StrictMode 설정.
 * 메인 스레드에서의 디스크/네트워크 접근과 리소스 누수를 감지하여 Logcat에 기록한다.
 * penaltyLog()만 사용하므로 앱이 크래시되지 않는다.
 */
class StrictModeInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        if (BuildConfig.DEBUG) {
            // 메인 스레드 정책: 디스크 읽기/쓰기, 네트워크 접근 감지
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            // VM 정책: Closeable/SQLite 객체 누수 감지
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .build()
            )
        }
    }

    // Timber 초기화 후 실행 (로그 출력을 위해)
    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(TimberInitializer::class.java)
}
