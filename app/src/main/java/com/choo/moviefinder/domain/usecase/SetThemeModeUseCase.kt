package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.ThemeMode
import com.choo.moviefinder.domain.repository.PreferencesRepository
import javax.inject.Inject

class SetThemeModeUseCase @Inject constructor(
    private val repository: PreferencesRepository
) {
    // 테마 모드를 변경하여 저장한다
    suspend operator fun invoke(themeMode: ThemeMode) = repository.setThemeMode(themeMode)
}