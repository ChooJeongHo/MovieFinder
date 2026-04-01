package com.choo.moviefinder.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movie_tags",
    indices = [
        Index(value = ["movieId"]),
        Index(value = ["movieId", "tagName"], unique = true)
    ]
)
data class MovieTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val movieId: Int,
    val tagName: String,
    val addedAt: Long = System.currentTimeMillis()
)
