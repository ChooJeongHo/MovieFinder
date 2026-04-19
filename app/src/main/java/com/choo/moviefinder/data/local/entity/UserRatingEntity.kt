package com.choo.moviefinder.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_ratings",
    indices = [Index(value = ["rating"])]
)
data class UserRatingEntity(
    @PrimaryKey val movieId: Int,
    val rating: Float,
    val ratedAt: Long = System.currentTimeMillis()
)
