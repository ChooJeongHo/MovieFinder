package com.choo.moviefinder.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoResponse(
    @SerialName("id") val id: Int,
    @SerialName("results") val results: List<VideoDto> = emptyList()
)

@Serializable
data class VideoDto(
    @SerialName("key") val key: String = "",
    @SerialName("site") val site: String = "",
    @SerialName("type") val type: String = "",
    @SerialName("official") val official: Boolean = false
)
