package com.choo.moviefinder.data.util

import com.choo.moviefinder.domain.model.DomainException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

class ApiExtTest {

    // --- toDomainException() ---

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<Any>(code, byteArrayOf().toResponseBody()))

    @Test
    fun `toDomainException maps HttpException 401 to Unauthorized`() {
        val result = httpException(401).toDomainException()
        assertTrue(result is DomainException.Unauthorized)
    }

    @Test
    fun `toDomainException maps HttpException 403 to Unauthorized`() {
        val result = httpException(403).toDomainException()
        assertTrue(result is DomainException.Unauthorized)
    }

    @Test
    fun `toDomainException maps HttpException 404 to NotFound`() {
        val result = httpException(404).toDomainException()
        assertTrue(result is DomainException.NotFound)
    }

    @Test
    fun `toDomainException maps HttpException 429 to RateLimited`() {
        val result = httpException(429).toDomainException()
        assertTrue(result is DomainException.RateLimited)
    }

    @Test
    fun `toDomainException maps HttpException 500 to ServerError`() {
        val result = httpException(500).toDomainException()
        assertTrue(result is DomainException.ServerError)
        assertEquals(500, (result as DomainException.ServerError).code)
    }

    @Test
    fun `toDomainException maps HttpException 503 to ServerError`() {
        val result = httpException(503).toDomainException()
        assertTrue(result is DomainException.ServerError)
        assertEquals(503, (result as DomainException.ServerError).code)
    }

    @Test
    fun `toDomainException maps SocketTimeoutException to Timeout`() {
        val result = SocketTimeoutException("timeout").toDomainException()
        assertTrue(result is DomainException.Timeout)
    }

    @Test
    fun `toDomainException maps SSLException to SslError`() {
        val result = SSLException("ssl error").toDomainException()
        assertTrue(result is DomainException.SslError)
    }

    @Test
    fun `toDomainException maps SSLHandshakeException to SslError`() {
        val result = SSLHandshakeException("handshake failed").toDomainException()
        assertTrue(result is DomainException.SslError)
    }

    @Test
    fun `toDomainException maps UnknownHostException to NetworkError`() {
        val result = UnknownHostException("no host").toDomainException()
        assertTrue(result is DomainException.NetworkError)
    }

    @Test
    fun `toDomainException maps generic IOException to NetworkError`() {
        val result = IOException("io error").toDomainException()
        assertTrue(result is DomainException.NetworkError)
    }

    @Test
    fun `toDomainException maps SerializationException to ParseError`() {
        val result = SerializationException("bad json").toDomainException()
        assertTrue(result is DomainException.ParseError)
    }

    @Test
    fun `toDomainException maps RuntimeException to Unknown`() {
        val result = RuntimeException("unexpected").toDomainException()
        assertTrue(result is DomainException.Unknown)
    }

    @Test
    fun `toDomainException preserves original exception as cause`() {
        val original = IOException("root cause")
        val result = original.toDomainException()
        assertSame(original, result.cause)
    }

    // --- safeApiCall() ---

    @Test
    fun `safeApiCall returns block result on success`() = runTest {
        val result = safeApiCall { 42 }
        assertEquals(42, result)
    }

    @Test
    fun `safeApiCall wraps IOException in DomainException`() = runTest {
        val thrown = runCatching {
            safeApiCall { throw IOException("io error") }
        }.exceptionOrNull()
        assertTrue(thrown is DomainException.NetworkError)
    }

    @Test
    fun `safeApiCall wraps HttpException in DomainException`() = runTest {
        val thrown = runCatching {
            safeApiCall { throw httpException(500) }
        }.exceptionOrNull()
        assertTrue(thrown is DomainException.ServerError)
    }

    @Test
    fun `safeApiCall rethrows CancellationException without wrapping`() = runTest {
        val thrown = runCatching {
            safeApiCall { throw CancellationException("cancelled") }
        }.exceptionOrNull()
        assertTrue(thrown is CancellationException)
    }

    @Test
    fun `safeApiCall wraps unknown exception in DomainException Unknown`() = runTest {
        val thrown = runCatching {
            safeApiCall { throw IllegalArgumentException("bad arg") }
        }.exceptionOrNull()
        assertTrue(thrown is DomainException.Unknown)
    }
}
