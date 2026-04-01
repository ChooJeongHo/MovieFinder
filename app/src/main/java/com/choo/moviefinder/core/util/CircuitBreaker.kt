package com.choo.moviefinder.core.util

import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class CircuitBreaker(
    private val name: String,
    private val failureThreshold: Int = 3,
    private val resetTimeoutMs: Long = 30_000L
) {
    enum class State { CLOSED, OPEN, HALF_OPEN }

    private val state = AtomicReference(State.CLOSED)
    private val failureCount = AtomicInteger(0)
    private val lastFailureTime = AtomicLong(0)

    @Synchronized
    fun recordSuccess() {
        if (state.get() == State.HALF_OPEN) {
            Timber.d("CircuitBreaker[$name]: HALF_OPEN → CLOSED (recovered)")
        }
        state.set(State.CLOSED)
        failureCount.set(0)
    }

    @Synchronized
    fun recordFailure() {
        val count = failureCount.incrementAndGet()
        lastFailureTime.set(System.currentTimeMillis())
        if (count >= failureThreshold && state.get() != State.OPEN) {
            state.set(State.OPEN)
            Timber.w("CircuitBreaker[$name]: → OPEN (failures: $count)")
        }
    }

    @Synchronized
    fun canProceed(): Boolean {
        return when (state.get()) {
            State.CLOSED -> true
            State.HALF_OPEN -> true
            State.OPEN -> {
                if (System.currentTimeMillis() - lastFailureTime.get() >= resetTimeoutMs) {
                    state.set(State.HALF_OPEN)
                    Timber.d("CircuitBreaker[$name]: OPEN → HALF_OPEN")
                    true
                } else {
                    false
                }
            }
        }
    }

    fun getState(): State = state.get()
}
