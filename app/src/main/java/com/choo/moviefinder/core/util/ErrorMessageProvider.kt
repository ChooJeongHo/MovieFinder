package com.choo.moviefinder.core.util

import android.content.Context
import com.choo.moviefinder.R
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorMessageProvider {

    fun getErrorMessage(context: Context, throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException,
            is java.net.ConnectException ->
                context.getString(R.string.error_network)

            is SocketTimeoutException ->
                context.getString(R.string.error_timeout)

            is HttpException ->
                context.getString(R.string.error_server)

            else ->
                context.getString(R.string.error_unknown)
        }
    }
}
