package com.choo.moviefinder.core.util

import android.os.Handler
import android.os.Looper
import timber.log.Timber

class AnrWatchdog : Thread("AnrWatchdog") {

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var responded = false

    init {
        isDaemon = true
    }

    // 메인 스레드에 주기적으로 ping을 보내고 응답이 없으면 ANR로 기록한다
    override fun run() {
        while (!isInterrupted) {
            responded = false
            mainHandler.post { responded = true }

            try {
                sleep(ANR_TIMEOUT)
            } catch (_: InterruptedException) {
                return
            }

            if (!responded) {
                val stackTrace = Looper.getMainLooper().thread.stackTrace
                    .joinToString("\n\t")
                Timber.e("ANR detected! Main thread blocked for ${ANR_TIMEOUT}ms")
                Timber.e("Main thread stack:\n\t$stackTrace")
            }
        }
    }

    companion object {
        private const val ANR_TIMEOUT = 5000L
    }
}
