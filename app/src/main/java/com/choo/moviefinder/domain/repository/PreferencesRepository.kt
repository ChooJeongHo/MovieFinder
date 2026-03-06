package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    // 현재 테마 모드 설정을 Flow로 반환한다
    fun getThemeMode(): Flow<ThemeMode>

    // 테마 모드를 변경하여 저장한다
    suspend fun setThemeMode(themeMode: ThemeMode)
}