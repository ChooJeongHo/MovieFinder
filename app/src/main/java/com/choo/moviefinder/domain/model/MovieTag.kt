package com.choo.moviefinder.domain.model

data class MovieTag(
    val id: Long,
    val movieId: Int,
    val tagName: String,
    val addedAt: Long
)
