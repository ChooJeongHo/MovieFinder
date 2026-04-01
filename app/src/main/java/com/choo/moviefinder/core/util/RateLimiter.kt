package com.choo.moviefinder.core.util

import java.util.concurrent.atomic.AtomicLong

class RateLimiter(private val minIntervalMs: Long = 2_000L) {
    private val lastActionTime = AtomicLong(0)

    fun tryAcquire(): Boolean {
        val now = System.currentTimeMillis()
        val last = lastActionTime.get()
        return if (now - last >= minIntervalMs) {
            lastActionTime.compareAndSet(last, now)
        } else {
            false
        }
    }
}
