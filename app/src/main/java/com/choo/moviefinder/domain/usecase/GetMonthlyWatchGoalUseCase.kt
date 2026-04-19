package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.PreferencesRepository
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@Reusable
class GetMonthlyWatchGoalUseCase @Inject constructor(
    private val repository: PreferencesRepository
) {
    // 월간 시청 목표 편수를 Flow로 조회한다
    operator fun invoke(): Flow<Int> = repository.getMonthlyWatchGoal()
}
