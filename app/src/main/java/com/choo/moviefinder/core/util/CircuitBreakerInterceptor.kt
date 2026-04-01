package com.choo.moviefinder.core.util

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

class CircuitBreakerInterceptor(
    private val circuitBreaker: CircuitBreaker
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!circuitBreaker.canProceed()) {
            Timber.w("CircuitBreakerInterceptor: Request to ${chain.request().url.host} blocked")
            throw IOException("Circuit breaker is OPEN - request blocked")
        }

        return try {
            val response = chain.proceed(chain.request())
            if (response.isSuccessful || response.code < 500) {
                circuitBreaker.recordSuccess()
            } else {
                circuitBreaker.recordFailure()
            }
            response
        } catch (e: IOException) {
            circuitBreaker.recordFailure()
            throw e
        }
    }
}
