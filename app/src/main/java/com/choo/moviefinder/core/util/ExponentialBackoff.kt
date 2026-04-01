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
                Timber.d("ExponentialBackoff: retry ${attempt + 1}/$maxRetries after ${currentDelay}ms — ${e.message}")
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
            }
        }
    }
    throw lastException ?: IllegalStateException("withExponentialBackoff failed with no exception")
}
