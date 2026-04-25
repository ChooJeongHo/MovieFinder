package com.choo.moviefinder.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "watch_history_genre",
    foreignKeys = [ForeignKey(
        entity = WatchHistoryEntity::class,
        parentColumns = ["rowId"],
        childColumns = ["watch_history_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("watch_history_id"),
        Index("genre_name"),
        Index(value = ["watch_history_id", "genre_name"], unique = true)
    ]
)
data class WatchHistoryGenreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "watch_history_id") val watchHistoryId: Long,
    @ColumnInfo(name = "genre_name") val genreName: String
)
