package com.choo.moviefinder.domain.model

data class WatchStats(
    val totalWatched: Int,
    val monthlyWatched: Int,
    val averageRating: Float?,
    val topGenres: List<GenreCount>
)

data class GenreCount(
    val name: String,
    val count: Int
)
