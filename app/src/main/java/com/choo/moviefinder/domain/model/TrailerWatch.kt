package com.choo.moviefinder.domain.model

data class TrailerWatch(
    val movieId: Int,
    val trailerKey: String,
    // epoch millis
    val watchedAt: Long
)
