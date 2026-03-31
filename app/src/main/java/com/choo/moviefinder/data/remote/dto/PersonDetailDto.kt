package com.choo.moviefinder.data.remote.dto

import com.choo.moviefinder.domain.model.PersonDetail
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonDetailDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String = "",
    @SerialName("biography") val biography: String = "",
    @SerialName("birthday") val birthday: String? = null,
    @SerialName("deathday") val deathday: String? = null,
    @SerialName("place_of_birth") val placeOfBirth: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("known_for_department") val knownForDepartment: String = ""
)

// PersonDetailDto를 도메인 PersonDetail 모델로 변환
fun PersonDetailDto.toDomain() = PersonDetail(
    id = id,
    name = name,
    biography = biography,
    birthday = birthday,
    deathday = deathday,
    placeOfBirth = placeOfBirth,
    profilePath = profilePath,
    knownForDepartment = knownForDepartment
)
