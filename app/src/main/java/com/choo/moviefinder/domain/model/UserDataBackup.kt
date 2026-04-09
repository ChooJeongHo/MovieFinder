package com.choo.moviefinder.domain.model

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class UserDataBackup(
    val version: Int = 1,
    val exportedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val favorites: List<BackupMovie> = emptyList(),
    val watchlist: List<BackupMovie> = emptyList(),
    val ratings: List<BackupRating> = emptyList(),
    val memos: List<BackupMemo> = emptyList(),
    val tags: List<BackupTag> = emptyList()
)

@Serializable
data class BackupMovie(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val voteAverage: Double,
    val overview: String,
    val releaseDate: String = "",
    val backdropPath: String? = null,
    val voteCount: Int = 0,
    val addedAt: Long = 0
)

@Serializable
data class BackupRating(
    val movieId: Int,
    val rating: Float
)

@Serializable
data class BackupMemo(
    val movieId: Int,
    val content: String,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

@Serializable
data class BackupTag(
    val movieId: Int,
    val tagName: String,
    val addedAt: Long = 0
)
