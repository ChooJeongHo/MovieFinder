package com.choo.moviefinder.domain.model

data class WatchlistReminder(
    val movieId: Int,
    val title: String,
    val reminderDate: Long?
)
