package com.choo.moviefinder.core.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import timber.log.Timber

suspend fun <T> withExponentialBackoff(
    maxRetries: Int = 3,
    initialDelayMs: Long = 1_000L,
    maxDelayMs: Long = 10_000L,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    require(maxRetries > 0) { "maxRetries must be positive, was $maxRetries" }
    require(initialDelayMs >= 0) { "initialDelayMs must be non-negative, was $initialDelayMs" }
    require(factor >= 1.0) { "factor must be >= 1.0, was $factor" }

    var currentDelay = initialDelayMs
    var lastException: Exception? = null

    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            lastException = e
            if (attempt < maxRetries - 1) {
                Timber.d("지수 백오프: ${currentDelay}ms 후 재시도 ${attempt + 1}/$maxRetries — ${e.message}")
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
            }
        }
    }
    throw lastException ?: IllegalStateException("withExponentialBackoff failed with no exception")
}
