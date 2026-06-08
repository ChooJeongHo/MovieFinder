package com.choo.moviefinder.data.util

import com.choo.moviefinder.domain.model.DomainException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

@PublishedApi
internal fun Exception.toDomainException(): DomainException = when (this) {
    is HttpException -> when (code()) {
        401, 403 -> DomainException.Unauthorized(this)
        404 -> DomainException.NotFound(this)
        429 -> DomainException.RateLimited(this)
        else -> DomainException.ServerError(code(), this)
    }
    is SocketTimeoutException -> DomainException.Timeout(this)
    is SSLException -> DomainException.SslError(this)
    is UnknownHostException, is IOException -> DomainException.NetworkError(this)
    is SerializationException -> DomainException.ParseError(this)
    else -> DomainException.Unknown(this)
}

suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): T {
    return try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw e.toDomainException()
    }
}
