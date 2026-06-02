package com.choo.moviefinder.core.util

import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Clock

class RateLimiter(private val minIntervalMs: Long = DEFAULT_COOLDOWN_MS) {
    private val lastActionTime = AtomicLong(0)

    fun tryAcquire(): Boolean {
        repeat(MAX_CAS_RETRIES) {
            val now = Clock.System.now().toEpochMilliseconds()
            val last = lastActionTime.get()
            if (now - last < minIntervalMs) return false
            if (lastActionTime.compareAndSet(last, now)) return true
        }
        return false
    }

    companion object {
        const val DEFAULT_COOLDOWN_MS = 2_000L
        private const val MAX_CAS_RETRIES = 5
    }
}
