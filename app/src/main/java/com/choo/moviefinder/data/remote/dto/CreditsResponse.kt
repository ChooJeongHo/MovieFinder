package com.choo.moviefinder.data.remote.dto

import com.choo.moviefinder.domain.model.Cast
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreditsResponse(
    @SerialName("id") val id: Int,
    @SerialName("cast") val cast: List<CastDto> = emptyList()
)

@Serializable
data class CastDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String = "",
    @SerialName("character") val character: String = "",
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("order") val order: Int = 0,
    @SerialName("adult") val adult: Boolean = false,
    @SerialName("gender") val gender: Int = 0,
    @SerialName("known_for_department") val knownForDepartment: String = "",
    @SerialName("original_name") val originalName: String = "",
    @SerialName("popularity") val popularity: Double = 0.0,
    @SerialName("cast_id") val castId: Int = 0,
    @SerialName("credit_id") val creditId: String = ""
)

fun CastDto.toDomain() = Cast(
    id = id,
    name = name,
    character = character,
    profilePath = profilePath
)