package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.PreferencesRepository
import javax.inject.Inject

class GetThemeModeUseCase @Inject constructor(
    private val repository: PreferencesRepository
) {
    operator fun invoke() = repository.getThemeMode()
}