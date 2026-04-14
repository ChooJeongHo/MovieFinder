package com.choo.moviefinder.core.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineExtTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun `successful block - onError not called`() = runTest {
        var errorCalled = false

        testScope.launchWithErrorHandler(
            onError = { errorCalled = true }
        ) {
            // no-op success
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!errorCalled)
    }

    @Test
    fun `exception in block - onError called with mapped ErrorType`() = runTest {
        var receivedError: ErrorType? = null

        testScope.launchWithErrorHandler(
            onError = { receivedError = it }
        ) {
            throw IllegalStateException("test error")
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(receivedError != null)
    }

    @Test
    fun `CancellationException - rethrown and onError not called`() = runTest {
        var errorCalled = false

        testScope.launchWithErrorHandler(
            onError = { errorCalled = true }
        ) {
            throw CancellationException("cancelled")
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!errorCalled)
    }

    @Test
    fun `network exception - onError called with NETWORK type`() = runTest {
        var receivedError: ErrorType? = null

        testScope.launchWithErrorHandler(
            onError = { receivedError = it }
        ) {
            throw java.net.UnknownHostException("no host")
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(receivedError == ErrorType.NETWORK)
    }

    // ── suspendRunCatching ────────────────────────────────────

    @Test
    fun `suspendRunCatching success - returns Result success`() {
        val result = suspendRunCatching { 42 }

        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `suspendRunCatching exception - returns Result failure`() {
        val exception = IllegalStateException("boom")

        val result = suspendRunCatching { throw exception }

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `suspendRunCatching CancellationException - rethrows instead of wrapping`() {
        var rethrown = false

        try {
            suspendRunCatching { throw CancellationException("cancelled") }
        } catch (e: CancellationException) {
            rethrown = true
        }

        assertTrue(rethrown)
    }

}
