package com.choo.moviefinder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.choo.moviefinder.data.local.entity.ScheduledReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledReminderDao {

    // 예약된 알림 전체 목록을 개봉일 오름차순으로 반환한다
    @Query("SELECT * FROM scheduled_reminders ORDER BY releaseDate ASC")
    fun getAllReminders(): Flow<List<ScheduledReminderEntity>>

    // 알림을 삽입하거나 교체한다
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ScheduledReminderEntity)

    // 특정 영화의 알림을 삭제한다
    @Query("DELETE FROM scheduled_reminders WHERE movieId = :movieId")
    suspend fun deleteReminder(movieId: Int)

    // 모든 예약 알림을 삭제한다
    @Query("DELETE FROM scheduled_reminders")
    suspend fun clearAll()
}
