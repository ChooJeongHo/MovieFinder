package com.choo.moviefinder.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CircuitBreakerTest {

    private lateinit var circuitBreaker: CircuitBreaker

    @Before
    fun setUp() {
        circuitBreaker = CircuitBreaker(name = "test", failureThreshold = 3, resetTimeoutMs = 30_000L)
    }

    @Test
    fun `initial state is CLOSED`() {
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState())
    }

    @Test
    fun `canProceed returns true in CLOSED state`() {
        assertTrue(circuitBreaker.canProceed())
    }

    @Test
    fun `failures below threshold keep state CLOSED`() {
        circuitBreaker.recordFailure()
        circuitBreaker.recordFailure()

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState())
        assertTrue(circuitBreaker.canProceed())
    }

    @Test
    fun `failures at threshold transition state to OPEN`() {
        circuitBreaker.recordFailure()
        circuitBreaker.recordFailure()
        circuitBreaker.recordFailure()

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState())
    }

    @Test
    fun `canProceed returns false in OPEN state before timeout`() {
        val cb = CircuitBreaker(name = "test", failureThreshold = 3, resetTimeoutMs = 60_000L)
        cb.recordFailure()
        cb.recordFailure()
        cb.recordFailure()

        assertFalse(cb.canProceed())
        assertEquals(CircuitBreaker.State.OPEN, cb.getState())
    }

    @Test
    fun `canProceed transitions OPEN to HALF_OPEN after timeout`() {
        val cb = CircuitBreaker(name = "test", failureThreshold = 3, resetTimeoutMs = 0L)
        cb.recordFailure()
        cb.recordFailure()
        cb.recordFailure()

        assertEquals(CircuitBreaker.State.OPEN, cb.getState())
        assertTrue(cb.canProceed())
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState())
    }

    @Test
    fun `canProceed returns true in HALF_OPEN state`() {
        val cb = CircuitBreaker(name = "test", failureThreshold = 3, resetTimeoutMs = 0L)
        cb.recordFailure()
        cb.recordFailure()
        cb.recordFailure()
        cb.canProceed() // transitions to HALF_OPEN

        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState())
        assertTrue(cb.canProceed())
    }

    @Test
    fun `recordSuccess in HALF_OPEN resets to CLOSED and clears failure count`() {
        val cb = CircuitBreaker(name = "test", failureThreshold = 3, resetTimeoutMs = 0L)
        cb.recordFailure()
        cb.recordFailure()
        cb.recordFailure()
        cb.canProceed() // transitions to HALF_OPEN

        cb.recordSuccess()

        assertEquals(CircuitBreaker.State.CLOSED, cb.getState())
        assertTrue(cb.canProceed())
    }

    @Test
    fun `recordSuccess in CLOSED state keeps state CLOSED`() {
        circuitBreaker.recordSuccess()

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState())
        assertTrue(circuitBreaker.canProceed())
    }

    @Test
    fun `after recordSuccess failure count resets and can open again after threshold`() {
        val cb = CircuitBreaker(name = "test", failureThreshold = 2, resetTimeoutMs = 0L)
        cb.recordFailure()
        cb.recordFailure()
        cb.canProceed() // OPEN -> HALF_OPEN
        cb.recordSuccess() // HALF_OPEN -> CLOSED, count reset

        cb.recordFailure()
        cb.recordFailure()

        assertEquals(CircuitBreaker.State.OPEN, cb.getState())
    }
}
