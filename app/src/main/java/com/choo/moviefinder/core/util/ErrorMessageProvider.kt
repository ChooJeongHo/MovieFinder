package com.choo.moviefinder.core.util

import android.content.Context
import com.choo.moviefinder.R
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

enum class ErrorType {
    NETWORK,
    TIMEOUT,
    SERVER,
    SSL,
    PARSE,
    UNKNOWN
}

object ErrorMessageProvider {

    // 예외를 분석하여 해당하는 ErrorType 열거형으로 매핑한다
    fun getErrorType(throwable: Throwable): ErrorType {
        return when (throwable) {
            is UnknownHostException,
            is java.net.ConnectException -> ErrorType.NETWORK

            is SocketTimeoutException -> ErrorType.TIMEOUT

            is SSLException -> ErrorType.SSL

            is HttpException -> ErrorType.SERVER

            is kotlinx.serialization.SerializationException -> ErrorType.PARSE

            is IOException -> ErrorType.NETWORK

            else -> ErrorType.UNKNOWN
        }
    }

    // ErrorType에 대응하는 사용자 친화적 에러 메시지 문자열을 반환한다
    fun getMessage(context: Context, errorType: ErrorType): String {
        return when (errorType) {
            ErrorType.NETWORK -> context.getString(R.string.error_network)
            ErrorType.TIMEOUT -> context.getString(R.string.error_timeout)
            ErrorType.SERVER -> context.getString(R.string.error_server)
            ErrorType.SSL -> context.getString(R.string.error_ssl)
            ErrorType.PARSE -> context.getString(R.string.error_parse)
            ErrorType.UNKNOWN -> context.getString(R.string.error_unknown)
        }
    }

    // 예외로부터 직접 사용자 친화적 에러 메시지를 반환한다
    fun getErrorMessage(context: Context, throwable: Throwable): String {
        return getMessage(context, getErrorType(throwable))
    }
}
