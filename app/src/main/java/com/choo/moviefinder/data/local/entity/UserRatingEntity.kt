package com.choo.moviefinder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_ratings")
data class UserRatingEntity(
    @PrimaryKey val movieId: Int,
    val rating: Float,
    val ratedAt: Long = System.currentTimeMillis()
)
