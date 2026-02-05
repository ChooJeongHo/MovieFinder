package com.choo.moviefinder.domain.model

data class MovieDetail(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val overview: String,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val runtime: Int?,
    val genres: List<Genre>,
    val tagline: String?
)

data class Genre(
    val id: Int,
    val name: String
)
