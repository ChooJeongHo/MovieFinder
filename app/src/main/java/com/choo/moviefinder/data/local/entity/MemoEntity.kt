package com.choo.moviefinder.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.choo.moviefinder.domain.model.Memo

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

fun MemoEntity.toDomain() = Memo(
    id = id,
    movieId = movieId,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt
)
