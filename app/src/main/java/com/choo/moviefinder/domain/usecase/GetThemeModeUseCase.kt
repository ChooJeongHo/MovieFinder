package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.PreferencesRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetThemeModeUseCase @Inject constructor(
    private val repository: PreferencesRepository
) {
    // 현재 테마 모드 설정을 Flow로 조회한다
    operator fun invoke() = repository.getThemeMode()
}