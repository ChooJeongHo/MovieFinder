package com.choo.moviefinder.domain.model

data class WatchStats(
    val totalWatched: Int,
    val monthlyWatched: Int,
    val averageRating: Float?,
    val topGenres: List<GenreCount>,
    val allGenreCounts: List<GenreCount>,
    val monthlyWatchCounts: List<MonthlyWatchCount>,
    val monthlyWatchGoal: Int = 0
)

data class GenreCount(
    val name: String,
    val count: Int
)

data class MonthlyWatchCount(
    val yearMonth: String,
    val count: Int
)
