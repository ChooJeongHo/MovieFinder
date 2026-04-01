package com.choo.moviefinder.core.util

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber

class AnrWatchdog : Thread("AnrWatchdog"), DefaultLifecycleObserver {

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var responded = false

    @Volatile
    private var isActive = true

    init {
        isDaemon = true
    }

    // 메인 스레드에 주기적으로 ping을 보내고 응답이 없으면 ANR로 기록한다
    override fun run() {
        while (!isInterrupted && isActive) {
            if (!isActive) {
                sleep(1000)
                continue
            }
            responded = false
            mainHandler.post { responded = true }

            try {
                sleep(ANR_TIMEOUT)
            } catch (_: InterruptedException) {
                return
            }

            if (!responded && isActive) {
                val stackTrace = Looper.getMainLooper().thread.stackTrace
                    .joinToString("\n\t")
                Timber.e("ANR detected! Main thread blocked for ${ANR_TIMEOUT}ms")
                Timber.e("Main thread stack:\n\t$stackTrace")
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        isActive = true
    }

    override fun onStop(owner: LifecycleOwner) {
        isActive = false
    }

    companion object {
        private const val ANR_TIMEOUT = 5000L
    }
}
