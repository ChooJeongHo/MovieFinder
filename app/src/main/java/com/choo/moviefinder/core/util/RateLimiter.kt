package com.choo.moviefinder.core.util

import java.util.concurrent.atomic.AtomicLong

class RateLimiter(private val minIntervalMs: Long = DEFAULT_COOLDOWN_MS) {
    private val lastActionTime = AtomicLong(0)

    fun tryAcquire(): Boolean {
        val now = System.currentTimeMillis()
        while (true) {
            val last = lastActionTime.get()
            if (now - last < minIntervalMs) return false
            if (lastActionTime.compareAndSet(last, now)) return true
        }
    }

    companion object {
        const val DEFAULT_COOLDOWN_MS = 2_000L
    }
}
