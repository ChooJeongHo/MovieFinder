package com.choo.moviefinder.data.remote.dto

import com.choo.moviefinder.domain.model.Genre
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenreListResponse(
    @SerialName("genres") val genres: List<GenreDto> = emptyList()
)

fun GenreListResponse.toDomain(): List<Genre> = genres.map {
    Genre(id = it.id, name = it.name)
}
