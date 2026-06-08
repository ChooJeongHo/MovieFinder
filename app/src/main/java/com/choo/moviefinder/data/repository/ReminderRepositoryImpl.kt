package com.choo.moviefinder.data.repository

import com.choo.moviefinder.core.notification.ReleaseNotificationScheduler
import com.choo.moviefinder.data.local.dao.ScheduledReminderDao
import com.choo.moviefinder.data.local.entity.ScheduledReminderEntity
import com.choo.moviefinder.data.local.entity.toDomain
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

    // DB에 ScheduledReminderEntity를 먼저 삽입한 뒤 WorkManager 알림을 예약한다.
    // 순서 중요: WorkManager를 먼저 등록하면 DB 삽입 실패 시 레코드 없는 잡이 남아
    // 사용자가 취소할 수 없는 고스트 알림이 발화될 수 있다.
    override suspend fun scheduleReminder(movieId: Int, movieTitle: String, releaseDate: String) {
        scheduledReminderDao.insertReminder(
            ScheduledReminderEntity(
                movieId = movieId,
                movieTitle = movieTitle,
                releaseDate = releaseDate,
                scheduledAt = System.currentTimeMillis()
            )
        )
        releaseNotificationScheduler.schedule(movieId, movieTitle, releaseDate)
    }

    // WorkManager 알림을 취소하고 DB에서 레코드를 삭제한다.
    // 순서 중요: WorkManager 취소를 먼저 해야 함 — DB를 먼저 지우면 취소 실패 시
    // 레코드 없는 잡이 남아 고스트 알림이 된다. 현재 순서의 실패 모드는
    // "발화되지 않을 알림이 목록에 남는 것"으로 재시도 가능해 더 안전하다.
    override suspend fun cancelReminder(movieId: Int) {
        releaseNotificationScheduler.cancel(movieId)
        scheduledReminderDao.deleteReminder(movieId)
    }

}
