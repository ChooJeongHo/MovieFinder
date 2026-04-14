package com.choo.moviefinder.core.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ExponentialBackoffTest {

    @Test
    fun `returns result immediately on first success`() = runTest {
        var callCount = 0

        val result = withExponentialBackoff(
            maxRetries = 3,
            initialDelayMs = 0L
        ) {
            callCount++
            "success"
        }

        assertEquals("success", result)
        assertEquals(1, callCount)
    }

    @Test
    fun `retries once then succeeds on second attempt`() = runTest {
        var callCount = 0

        val result = withExponentialBackoff(
            maxRetries = 3,
            initialDelayMs = 0L
        ) {
            callCount++
            if (callCount < 2) throw IllegalStateException("fail")
            "recovered"
        }

        assertEquals("recovered", result)
        assertEquals(2, callCount)
    }

    @Test
    fun `throws last exception when all retries exhausted`() = runTest {
        var callCount = 0
        val expectedException = RuntimeException("always fails")

        try {
            withExponentialBackoff(
                maxRetries = 3,
                initialDelayMs = 0L
            ) {
                callCount++
                throw expectedException
            }
            fail("Expected exception was not thrown")
        } catch (e: RuntimeException) {
            assertEquals("always fails", e.message)
        }

        assertEquals(3, callCount)
    }

    @Test
    fun `rethrows CancellationException immediately without retrying`() = runTest {
        var callCount = 0

        try {
            withExponentialBackoff(
                maxRetries = 3,
                initialDelayMs = 0L
            ) {
                callCount++
                throw CancellationException("cancelled")
            }
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            assertEquals("cancelled", e.message)
        }

        assertEquals(1, callCount)
    }

    @Test
    fun `invokes block exactly maxRetries times on repeated failure`() = runTest {
        var callCount = 0

        try {
            withExponentialBackoff(
                maxRetries = 5,
                initialDelayMs = 0L
            ) {
                callCount++
                throw IllegalStateException("fail")
            }
        } catch (_: IllegalStateException) {}

        assertEquals(5, callCount)
    }

    @Test
    fun `succeeds on last allowed attempt`() = runTest {
        var callCount = 0

        val result = withExponentialBackoff(
            maxRetries = 3,
            initialDelayMs = 0L
        ) {
            callCount++
            if (callCount < 3) throw IllegalStateException("not yet")
            "last chance"
        }

        assertEquals("last chance", result)
        assertEquals(3, callCount)
    }

    // ── require 입력 검증 ─────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `maxRetries zero throws IllegalArgumentException`() = runTest {
        withExponentialBackoff(maxRetries = 0) { "unused" }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative initialDelayMs throws IllegalArgumentException`() = runTest {
        withExponentialBackoff(initialDelayMs = -1L) { "unused" }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `factor less than 1 throws IllegalArgumentException`() = runTest {
        withExponentialBackoff(factor = 0.5) { "unused" }
    }
}
