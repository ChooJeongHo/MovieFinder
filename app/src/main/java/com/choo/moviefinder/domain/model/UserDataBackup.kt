package com.choo.moviefinder.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDataBackup(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val favorites: List<BackupMovie> = emptyList(),
    val watchlist: List<BackupMovie> = emptyList(),
    val ratings: List<BackupRating> = emptyList(),
    val memos: List<BackupMemo> = emptyList()
)

@Serializable
data class BackupMovie(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val voteAverage: Double,
    val overview: String
)

@Serializable
data class BackupRating(
    val movieId: Int,
    val rating: Float
)

@Serializable
data class BackupMemo(
    val movieId: Int,
    val content: String
)
