package com.choo.moviefinder.domain.model

data class CollectionDetail(
    val id: Int,
    val name: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val movies: List<Movie>
)
