package com.choo.moviefinder.core.util

import android.content.Context
import com.choo.moviefinder.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

class ErrorMessageProviderTest {

    @Test
    fun `getErrorType returns NETWORK for UnknownHostException`() {
        val result = ErrorMessageProvider.getErrorType(UnknownHostException("no host"))
        assertEquals(ErrorType.NETWORK, result)
    }

    @Test
    fun `getErrorType returns NETWORK for ConnectException`() {
        val result = ErrorMessageProvider.getErrorType(ConnectException("connection refused"))
        assertEquals(ErrorType.NETWORK, result)
    }

    @Test
    fun `getErrorType returns TIMEOUT for SocketTimeoutException`() {
        val result = ErrorMessageProvider.getErrorType(SocketTimeoutException("timeout"))
        assertEquals(ErrorType.TIMEOUT, result)
    }

    @Test
    fun `getErrorType returns SERVER for HttpException`() {
        val response = Response.error<Any>(500, byteArrayOf().toResponseBody())
        val result = ErrorMessageProvider.getErrorType(HttpException(response))
        assertEquals(ErrorType.SERVER, result)
    }

    @Test
    fun `getErrorType returns UNKNOWN for RuntimeException`() {
        val result = ErrorMessageProvider.getErrorType(RuntimeException("unknown"))
        assertEquals(ErrorType.UNKNOWN, result)
    }

    @Test
    fun `getErrorType returns UNKNOWN for IllegalStateException`() {
        val result = ErrorMessageProvider.getErrorType(IllegalStateException("bad state"))
        assertEquals(ErrorType.UNKNOWN, result)
    }

    @Test
    fun `getErrorType returns SSL for SSLException`() {
        val result = ErrorMessageProvider.getErrorType(SSLException("ssl error"))
        assertEquals(ErrorType.SSL, result)
    }

    @Test
    fun `getErrorType returns SSL for SSLHandshakeException`() {
        val result = ErrorMessageProvider.getErrorType(SSLHandshakeException("handshake failed"))
        assertEquals(ErrorType.SSL, result)
    }

    @Test
    fun `getErrorType returns PARSE for SerializationException`() {
        val result = ErrorMessageProvider.getErrorType(
            kotlinx.serialization.SerializationException("bad json")
        )
        assertEquals(ErrorType.PARSE, result)
    }

    @Test
    fun `getErrorType returns NETWORK for generic IOException`() {
        val result = ErrorMessageProvider.getErrorType(IOException("io error"))
        assertEquals(ErrorType.NETWORK, result)
    }

    // --- getMessage(Context, ErrorType) ---

    private fun mockContext(resId: Int, returnValue: String): Context = mockk<Context>().also {
        every { it.getString(resId) } returns returnValue
    }

    @Test
    fun `getMessage with NETWORK error type requests error_network string`() {
        val context = mockk<Context>()
        every { context.getString(R.string.error_network) } returns "network error"

        val result = ErrorMessageProvider.getMessage(context, ErrorType.NETWORK)

        assertEquals("network error", result)
        verify { context.getString(R.string.error_network) }
    }

    @Test
    fun `getMessage with TIMEOUT error type requests error_timeout string`() {
        val context = mockk<Context>()
        every { context.getString(R.string.error_timeout) } returns "timeout error"

        val result = ErrorMessageProvider.getMessage(context, ErrorType.TIMEOUT)

        assertEquals("timeout error", result)
        verify { context.getString(R.string.error_timeout) }
    }

    @Test
    fun `getMessage with SERVER error type requests error_server string`() {
        val context = mockk<Context>()
        every { context.getString(R.string.error_server) } returns "server error"

        val result = ErrorMessageProvider.getMessage(context, ErrorType.SERVER)

        assertEquals("server error", result)
        verify { context.getString(R.string.error_server) }
    }

    @Test
    fun `getMessage with SSL error type requests error_ssl string`() {
        val context = mockk<Context>()
        every { context.getString(R.string.error_ssl) } returns "ssl error"

        val result = ErrorMessageProvider.getMessage(context, ErrorType.SSL)

        assertEquals("ssl error", result)
        verify { context.getString(R.string.error_ssl) }
    }

    @Test
    fun `getMessage with PARSE error type requests error_parse string`() {
        val context = mockk<Context>()
        every { context.getString(R.string.error_parse) } returns "parse error"

        val result = ErrorMessageProvider.getMessage(context, ErrorType.PARSE)

        assertEquals("parse error", result)
        verify { context.getString(R.string.error_parse) }
    }

    @Test
    fun `getMessage with UNKNOWN error type requests error_unknown string`() {
        val context = mockk<Context>()
        every { context.getString(R.string.error_unknown) } returns "unknown error"

        val result = ErrorMessageProvider.getMessage(context, ErrorType.UNKNOWN)

        assertEquals("unknown error", result)
        verify { context.getString(R.string.error_unknown) }
    }

    // --- getMessage(Context, Throwable) ---

    @Test
    fun `getMessage with IOException delegates to NETWORK error type`() {
        val context = mockk<Context>()
        every { context.getString(R.string.error_network) } returns "network error"

        val result = ErrorMessageProvider.getMessage(context, IOException("io error"))

        assertEquals("network error", result)
        verify { context.getString(R.string.error_network) }
    }

    @Test
    fun `getMessage with SocketTimeoutException delegates to TIMEOUT error type`() {
        val context = mockk<Context>()
        every { context.getString(R.string.error_timeout) } returns "timeout error"

        val result = ErrorMessageProvider.getMessage(context, SocketTimeoutException("timeout"))

        assertEquals("timeout error", result)
        verify { context.getString(R.string.error_timeout) }
    }

    @Test
    fun `getMessage with RuntimeException delegates to UNKNOWN error type`() {
        val context = mockk<Context>()
        every { context.getString(R.string.error_unknown) } returns "unknown error"

        val result = ErrorMessageProvider.getMessage(context, RuntimeException("unexpected"))

        assertEquals("unknown error", result)
        verify { context.getString(R.string.error_unknown) }
    }

    @Test
    fun `getMessage with HttpException delegates to SERVER error type`() {
        val context = mockk<Context>()
        every { context.getString(R.string.error_server) } returns "server error"
        val response = Response.error<Any>(500, byteArrayOf().toResponseBody())

        val result = ErrorMessageProvider.getMessage(context, HttpException(response))

        assertEquals("server error", result)
        verify { context.getString(R.string.error_server) }
    }

    @Test
    fun `getErrorType returns UNAUTHORIZED for HttpException 401`() {
        val response = Response.error<Any>(401, byteArrayOf().toResponseBody())
        val result = ErrorMessageProvider.getErrorType(HttpException(response))
        assertEquals(ErrorType.UNAUTHORIZED, result)
    }

    @Test
    fun `getErrorType returns UNAUTHORIZED for HttpException 403`() {
        val response = Response.error<Any>(403, byteArrayOf().toResponseBody())
        val result = ErrorMessageProvider.getErrorType(HttpException(response))
        assertEquals(ErrorType.UNAUTHORIZED, result)
    }

    @Test
    fun `getErrorType returns NOT_FOUND for HttpException 404`() {
        val response = Response.error<Any>(404, byteArrayOf().toResponseBody())
        val result = ErrorMessageProvider.getErrorType(HttpException(response))
        assertEquals(ErrorType.NOT_FOUND, result)
    }

    @Test
    fun `getErrorType returns RATE_LIMITED for HttpException 429`() {
        val response = Response.error<Any>(429, byteArrayOf().toResponseBody())
        val result = ErrorMessageProvider.getErrorType(HttpException(response))
        assertEquals(ErrorType.RATE_LIMITED, result)
    }

    @Test
    fun `getMessage with UNAUTHORIZED error type requests error_unauthorized string`() {
        val context = mockk<Context>()
        every { context.getString(R.string.error_unauthorized) } returns "unauthorized"

        val result = ErrorMessageProvider.getMessage(context, ErrorType.UNAUTHORIZED)

        assertEquals("unauthorized", result)
        verify { context.getString(R.string.error_unauthorized) }
    }

    @Test
    fun `getMessage with NOT_FOUND error type requests error_not_found string`() {
        val context = mockk<Context>()
        every { context.getString(R.string.error_not_found) } returns "not found"

        val result = ErrorMessageProvider.getMessage(context, ErrorType.NOT_FOUND)

        assertEquals("not found", result)
        verify { context.getString(R.string.error_not_found) }
    }

    @Test
    fun `getMessage with RATE_LIMITED error type requests error_rate_limited string`() {
        val context = mockk<Context>()
        every { context.getString(R.string.error_rate_limited) } returns "rate limited"

        val result = ErrorMessageProvider.getMessage(context, ErrorType.RATE_LIMITED)

        assertEquals("rate limited", result)
        verify { context.getString(R.string.error_rate_limited) }
    }
}