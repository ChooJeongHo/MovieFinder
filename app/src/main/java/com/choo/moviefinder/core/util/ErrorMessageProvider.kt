package com.choo.moviefinder.core.util

import android.content.Context
import com.choo.moviefinder.R
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

enum class ErrorType {
    NETWORK,
    TIMEOUT,
    SERVER,
    UNKNOWN
}

object ErrorMessageProvider {

    fun getErrorType(throwable: Throwable): ErrorType {
        return when (throwable) {
            is UnknownHostException,
            is java.net.ConnectException -> ErrorType.NETWORK

            is SocketTimeoutException -> ErrorType.TIMEOUT

            is HttpException -> ErrorType.SERVER

            else -> ErrorType.UNKNOWN
        }
    }

    fun getMessage(context: Context, errorType: ErrorType): String {
        return when (errorType) {
            ErrorType.NETWORK -> context.getString(R.string.error_network)
            ErrorType.TIMEOUT -> context.getString(R.string.error_timeout)
            ErrorType.SERVER -> context.getString(R.string.error_server)
            ErrorType.UNKNOWN -> context.getString(R.string.error_unknown)
        }
    }

    fun getErrorMessage(context: Context, throwable: Throwable): String {
        return getMessage(context, getErrorType(throwable))
    }
}
