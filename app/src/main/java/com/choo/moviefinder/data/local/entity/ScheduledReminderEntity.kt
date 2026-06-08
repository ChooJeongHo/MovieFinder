package com.choo.moviefinder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.choo.moviefinder.domain.model.ScheduledReminder

@Entity(tableName = "scheduled_reminders")
data class ScheduledReminderEntity(
    @PrimaryKey val movieId: Int,
    val movieTitle: String,
    // "yyyy-MM-dd"
    val releaseDate: String,
    // System.currentTimeMillis()
    val scheduledAt: Long
)

fun ScheduledReminderEntity.toDomain() = ScheduledReminder(
    movieId = movieId,
    movieTitle = movieTitle,
    releaseDate = releaseDate,
    scheduledAt = scheduledAt
)
