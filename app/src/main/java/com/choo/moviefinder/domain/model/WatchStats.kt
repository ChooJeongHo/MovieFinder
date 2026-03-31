package com.choo.moviefinder.domain.model

data class WatchStats(
    val totalWatched: Int,
    val monthlyWatched: Int,
    val averageRating: Float?,
    val topGenres: List<GenreCount>,
    val allGenreCounts: List<GenreCount>,
    val monthlyWatchCounts: List<MonthlyWatchCount>,
    val monthlyWatchGoal: Int = 0,
    val ratingDistribution: List<RatingBucket> = emptyList(),
    val dailyWatchCounts: List<DailyWatchCount> = emptyList()
)

data class GenreCount(
    val name: String,
    val count: Int
)

data class MonthlyWatchCount(
    val yearMonth: String,
    val count: Int
)

data class RatingBucket(
    val rating: Float,
    val count: Int
)

data class DailyWatchCount(
    val date: String,
    val count: Int
)
