package com.choo.moviefinder.data.repository

import com.choo.moviefinder.core.notification.ReleaseNotificationScheduler
import com.choo.moviefinder.data.local.dao.ScheduledReminderDao
import com.choo.moviefinder.data.local.entity.ScheduledReminderEntity
import com.choo.moviefinder.domain.model.ScheduledReminder
import com.choo.moviefinder.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val scheduledReminderDao: ScheduledReminderDao,
    private val releaseNotificationScheduler: ReleaseNotificationScheduler
) : ReminderRepository {

    // 예약된 알림 전체를 도메인 모델로 변환하여 Flow로 반환한다
    override fun getAllReminders(): Flow<List<ScheduledReminder>> =
        scheduledReminderDao.getAllReminders().map { entities ->
            entities.map { it.toDomain() }
        }

    // WorkManager 알림을 예약하고 DB에 ScheduledReminderEntity를 삽입한다
    override suspend fun scheduleReminder(movieId: Int, movieTitle: String, releaseDate: String) {
        releaseNotificationScheduler.schedule(movieId, movieTitle, releaseDate)
        scheduledReminderDao.insertReminder(
            ScheduledReminderEntity(
                movieId = movieId,
                movieTitle = movieTitle,
                releaseDate = releaseDate,
                scheduledAt = System.currentTimeMillis()
            )
        )
    }

    // WorkManager 알림을 취소하고 DB에서 레코드를 삭제한다
    override suspend fun cancelReminder(movieId: Int) {
        releaseNotificationScheduler.cancel(movieId)
        scheduledReminderDao.deleteReminder(movieId)
    }

    private fun ScheduledReminderEntity.toDomain() = ScheduledReminder(
        movieId = movieId,
        movieTitle = movieTitle,
        releaseDate = releaseDate,
        scheduledAt = scheduledAt
    )
}
