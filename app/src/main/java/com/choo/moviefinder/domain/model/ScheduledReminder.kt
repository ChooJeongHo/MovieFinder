package com.choo.moviefinder.domain.model

data class ScheduledReminder(
    val movieId: Int,
    val movieTitle: String,
    // "yyyy-MM-dd"
    val releaseDate: String,
    // epoch millis
    val scheduledAt: Long
)
