package com.choo.moviefinder.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.choo.moviefinder.domain.model.Movie

@Entity(
    tableName = "watch_history",
    indices = [Index(value = ["watchedAt"])]
)
data class WatchHistoryEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val watchedAt: Long = System.currentTimeMillis()
)

fun WatchHistoryEntity.toDomain() = Movie(
    id = id,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    overview = "",
    releaseDate = "",
    voteAverage = voteAverage,
    voteCount = 0
)

fun Movie.toWatchHistoryEntity() = WatchHistoryEntity(
    id = id,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    voteAverage = voteAverage
)
