package com.choo.moviefinder.data.remote.dto

import com.choo.moviefinder.domain.model.PersonSearchItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonSearchResponse(
    @SerialName("results") val results: List<PersonSearchResult> = emptyList(),
    @SerialName("total_results") val totalResults: Int = 0
)

@Serializable
data class PersonSearchResult(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String = "",
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("known_for_department") val knownForDepartment: String = "",
    @SerialName("known_for") val knownFor: List<KnownForMovie> = emptyList()
)

@Serializable
data class KnownForMovie(
    @SerialName("id") val id: Int = 0,
    @SerialName("title") val title: String? = null,
    @SerialName("name") val name: String? = null
)

// PersonSearchResult를 도메인 PersonSearchItem 모델로 변환
fun PersonSearchResult.toDomain() = PersonSearchItem(
    id = id,
    name = name,
    profilePath = profilePath,
    knownForDepartment = knownForDepartment,
    knownForTitles = knownFor.mapNotNull { it.title ?: it.name }
        .take(3)
        .joinToString(", ")
)
