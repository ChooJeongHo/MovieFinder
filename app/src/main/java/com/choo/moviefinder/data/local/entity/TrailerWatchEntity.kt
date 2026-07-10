package com.choo.moviefinder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.choo.moviefinder.domain.model.TrailerWatch

@Entity(tableName = "trailer_watches")
data class TrailerWatchEntity(
    @PrimaryKey val movieId: Int,
    val trailerKey: String,
    // System.currentTimeMillis()
    val watchedAt: Long
)

fun TrailerWatchEntity.toDomain() = TrailerWatch(
    movieId = movieId,
    trailerKey = trailerKey,
    watchedAt = watchedAt
)
