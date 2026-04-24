package com.choo.moviefinder.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RequestTokenResponse(
    @SerialName("request_token") val requestToken: String,
    @SerialName("success") val success: Boolean = false,
    @SerialName("status_message") val statusMessage: String = ""
)

@Serializable
data class AccessTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("success") val success: Boolean = false,
    @SerialName("status_message") val statusMessage: String = ""
)

@Serializable
data class SessionResponse(
    @SerialName("session_id") val sessionId: String,
    @SerialName("success") val success: Boolean = false
)

@Serializable
data class RatingRequest(
    @SerialName("value") val value: Float
)

@Serializable
data class RatingResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("status_message") val statusMessage: String = ""
)
