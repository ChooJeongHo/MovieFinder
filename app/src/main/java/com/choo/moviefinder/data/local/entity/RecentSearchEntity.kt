package com.choo.moviefinder.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recent_searches",
    indices = [Index(value = ["timestamp"])]
)
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)