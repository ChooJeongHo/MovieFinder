package com.choo.moviefinder.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memos",
    indices = [Index(value = ["movieId"])]
)
data class MemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val movieId: Int,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
