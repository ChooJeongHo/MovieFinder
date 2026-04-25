package com.choo.moviefinder.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchProviderDto(
    @SerialName("provider_id") val providerId: Int,
    @SerialName("provider_name") val providerName: String,
    @SerialName("logo_path") val logoPath: String? = null
)

@Serializable
data class WatchProviderRegionResult(
    @SerialName("flatrate") val flatrate: List<WatchProviderDto> = emptyList(),
    @SerialName("rent") val rent: List<WatchProviderDto> = emptyList(),
    @SerialName("buy") val buy: List<WatchProviderDto> = emptyList()
)

@Serializable
data class WatchProviderResponse(
    @SerialName("id") val id: Int,
    @SerialName("results") val results: Map<String, WatchProviderRegionResult> = emptyMap()
)
