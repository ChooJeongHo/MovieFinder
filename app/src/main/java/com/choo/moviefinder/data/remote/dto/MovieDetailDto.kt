package com.choo.moviefinder.data.remote.dto

import com.choo.moviefinder.domain.model.Genre
import com.choo.moviefinder.domain.model.MovieDetail
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovieDetailDto(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("overview") val overview: String = "",
    @SerialName("release_date") val releaseDate: String = "",
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("runtime") val runtime: Int? = null,
    @SerialName("genres") val genres: List<GenreDto> = emptyList(),
    @SerialName("tagline") val tagline: String? = null,
    @SerialName("adult") val adult: Boolean = false,
    @SerialName("budget") val budget: Long = 0,
    @SerialName("homepage") val homepage: String? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("original_language") val originalLanguage: String = "",
    @SerialName("original_title") val originalTitle: String = "",
    @SerialName("popularity") val popularity: Double = 0.0,
    @SerialName("revenue") val revenue: Long = 0,
    @SerialName("status") val status: String = "",
    @SerialName("video") val video: Boolean = false
)

@Serializable
data class GenreDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String
)

fun MovieDetailDto.toDomain() = MovieDetail(
    id = id,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount,
    runtime = runtime,
    genres = genres.map { Genre(id = it.id, name = it.name) },
    tagline = tagline
)