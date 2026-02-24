package com.choo.moviefinder.core.util

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
        val response = Response.error<Any>(500, okhttp3.ResponseBody.create(null, byteArrayOf()))
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
}