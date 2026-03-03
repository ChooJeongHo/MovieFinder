package com.choo.moviefinder.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReleaseDateResponse(
    @SerialName("id") val id: Int,
    @SerialName("results") val results: List<ReleaseDateResult> = emptyList()
)

@Serializable
data class ReleaseDateResult(
    @SerialName("iso_3166_1") val iso31661: String,
    @SerialName("release_dates") val releaseDates: List<ReleaseDateInfo> = emptyList()
)

@Serializable
data class ReleaseDateInfo(
    @SerialName("certification") val certification: String = "",
    @SerialName("release_date") val releaseDate: String = "",
    @SerialName("type") val type: Int = 0
)
