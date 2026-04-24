package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.ScheduledReminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {

    // 예약된 알림 전체 목록을 Flow로 반환한다
    fun getAllReminders(): Flow<List<ScheduledReminder>>

    // WorkManager 알림을 예약하고 DB에 기록한다
    suspend fun scheduleReminder(movieId: Int, movieTitle: String, releaseDate: String)

    // WorkManager 알림을 취소하고 DB에서 삭제한다
    suspend fun cancelReminder(movieId: Int)
}
