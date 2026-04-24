package com.choo.moviefinder.data.remote.dto

import com.choo.moviefinder.domain.model.CollectionDetail
import com.choo.moviefinder.domain.model.Movie
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BelongsToCollectionDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String = ""
)

@Serializable
data class CollectionDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String = "",
    @SerialName("overview") val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("parts") val parts: List<MovieDto> = emptyList()
)

fun CollectionDto.toDomain() = CollectionDetail(
    id = id,
    name = name,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    movies = parts.map {
        Movie(
            id = it.id,
            title = it.title,
            posterPath = it.posterPath,
            backdropPath = it.backdropPath,
            overview = it.overview,
            releaseDate = it.releaseDate,
            voteAverage = it.voteAverage,
            voteCount = it.voteCount
        )
    }.sortedBy { it.releaseDate }
)
