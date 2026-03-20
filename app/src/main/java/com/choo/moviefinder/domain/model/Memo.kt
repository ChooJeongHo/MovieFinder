package com.choo.moviefinder.domain.model

data class Memo(
    val id: Long,
    val movieId: Int,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)
