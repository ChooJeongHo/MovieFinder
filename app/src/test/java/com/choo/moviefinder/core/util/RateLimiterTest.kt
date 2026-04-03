package com.choo.moviefinder.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimiterTest {

    @Test
    fun `first tryAcquire returns true`() {
        val rateLimiter = RateLimiter(minIntervalMs = 2_000L)

        assertTrue(rateLimiter.tryAcquire())
    }

    @Test
    fun `immediate second tryAcquire returns false while in cooldown`() {
        val rateLimiter = RateLimiter(minIntervalMs = 2_000L)
        rateLimiter.tryAcquire()

        assertFalse(rateLimiter.tryAcquire())
    }

    @Test
    fun `consecutive calls both return true when minIntervalMs is zero`() {
        val rateLimiter = RateLimiter(minIntervalMs = 0L)

        assertTrue(rateLimiter.tryAcquire())
        assertTrue(rateLimiter.tryAcquire())
    }

    @Test
    fun `third consecutive call also returns false during cooldown`() {
        val rateLimiter = RateLimiter(minIntervalMs = 5_000L)
        rateLimiter.tryAcquire()

        assertFalse(rateLimiter.tryAcquire())
        assertFalse(rateLimiter.tryAcquire())
    }

    @Test
    fun `default minIntervalMs causes second call to fail`() {
        val rateLimiter = RateLimiter()
        rateLimiter.tryAcquire()

        assertFalse(rateLimiter.tryAcquire())
    }
}
