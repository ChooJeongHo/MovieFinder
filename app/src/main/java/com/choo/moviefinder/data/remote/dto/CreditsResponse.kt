package com.choo.moviefinder.data.remote.dto

import com.choo.moviefinder.domain.model.Cast
import com.choo.moviefinder.domain.model.Credits
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreditsResponse(
    @SerialName("id") val id: Int,
    @SerialName("cast") val cast: List<CastDto> = emptyList(),
    @SerialName("crew") val crew: List<CrewDto> = emptyList()
)

@Serializable
data class CrewDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String = "",
    @SerialName("job") val job: String = "",
    @SerialName("department") val department: String = "",
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("credit_id") val creditId: String = ""
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

// CastDto를 도메인 Cast 모델로 변환
fun CastDto.toDomain() = Cast(
    id = id,
    name = name,
    character = character,
    profilePath = profilePath
)

// CreditsResponse를 도메인 Credits 모델로 변환 (감독만 추출)
fun CreditsResponse.toDomain() = Credits(
    cast = cast.map { it.toDomain() },
    directors = crew.filter { it.job == "Director" }.map { it.name }
)