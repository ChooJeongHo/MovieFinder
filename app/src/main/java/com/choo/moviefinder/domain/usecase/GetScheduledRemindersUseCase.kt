package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.ScheduledReminder
import com.choo.moviefinder.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetScheduledRemindersUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    // 예약된 알림 전체를 Flow로 반환한다
    operator fun invoke(): Flow<List<ScheduledReminder>> =
        reminderRepository.getAllReminders()
}
