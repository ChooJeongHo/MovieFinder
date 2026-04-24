package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.ReminderRepository
import javax.inject.Inject

class CancelReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    // WorkManager 알림을 취소하고 DB에서 레코드를 삭제한다
    suspend operator fun invoke(movieId: Int) {
        reminderRepository.cancelReminder(movieId)
    }
}
