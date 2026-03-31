package com.choo.moviefinder.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonCreditsResponse(
    @SerialName("cast") val cast: List<MovieDto> = emptyList()
)
