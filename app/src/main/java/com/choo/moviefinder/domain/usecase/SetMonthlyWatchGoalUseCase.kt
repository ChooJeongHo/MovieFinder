package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.PreferencesRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class SetMonthlyWatchGoalUseCase @Inject constructor(
    private val repository: PreferencesRepository
) {
    // 월간 시청 목표 편수를 저장한다 (0~100 범위만 허용)
    suspend operator fun invoke(goal: Int) {
        require(goal in 0..100) { "Monthly watch goal must be between 0 and 100" }
        repository.setMonthlyWatchGoal(goal)
    }
}
