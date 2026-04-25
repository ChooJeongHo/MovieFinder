package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.PreferencesRepository
import javax.inject.Inject

class CompleteOnboardingUseCase @Inject constructor(
    private val repository: PreferencesRepository
) {
    suspend operator fun invoke() = repository.setOnboardingCompleted()
}
