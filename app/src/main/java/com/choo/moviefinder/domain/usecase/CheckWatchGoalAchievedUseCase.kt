@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.core.util.currentMonthStartMillis
import com.choo.moviefinder.core.util.currentYearMonth
import com.choo.moviefinder.domain.repository.PreferencesRepository
import com.choo.moviefinder.domain.repository.WatchHistoryRepository
import dagger.Reusable
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@Reusable
class CheckWatchGoalAchievedUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val watchHistoryRepository: WatchHistoryRepository
) {
    // 이번 달 시청 목표 달성 여부를 확인한다.
    // 목표 미설정·미달성·이미 알림 전송 시 false, 처음 달성 시 true를 반환한다.
    suspend operator fun invoke(): Boolean {
        val goal = preferencesRepository.getMonthlyWatchGoal().first()
        if (goal <= 0) return false

        val currentCount = watchHistoryRepository.getWatchedCountSince(currentMonthStartMillis()).first()
        if (currentCount < goal) return false

        val yearMonth = currentYearMonth()
        val lastNotified = preferencesRepository.getLastGoalNotifiedMonth().first()
        if (lastNotified == yearMonth) return false

        preferencesRepository.setLastGoalNotifiedMonth(yearMonth)
        return true
    }
}
