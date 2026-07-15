package com.choo.moviefinder.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "helpful_reviews", indices = [Index("movieId")])
data class HelpfulReviewEntity(
    @PrimaryKey val reviewId: String,
    val movieId: Int,
    // kotlin.time.Clock.System.now().toEpochMilliseconds()
    val markedAt: Long
)
