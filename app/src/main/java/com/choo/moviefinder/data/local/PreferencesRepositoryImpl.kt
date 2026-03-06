package com.choo.moviefinder.data.local

import androidx.datastore.core.DataStore
import com.choo.moviefinder.domain.model.ThemeMode
import com.choo.moviefinder.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class PreferencesRepositoryImpl @Inject constructor(
    private val userSettingsStore: DataStore<UserSettings>
) : PreferencesRepository {

    // 저장된 테마 모드를 Flow로 조회 (잘못된 값은 SYSTEM으로 복구)
    override fun getThemeMode(): Flow<ThemeMode> {
        return userSettingsStore.data.map { settings ->
            try {
                ThemeMode.valueOf(settings.themeMode)
            } catch (e: IllegalArgumentException) {
                Timber.w(e, "잘못된 테마 설정값: %s, 기본값(SYSTEM)으로 복구", settings.themeMode)
                ThemeMode.SYSTEM
            }
        }
    }

    // 테마 모드를 DataStore에 저장
    override suspend fun setThemeMode(themeMode: ThemeMode) {
        userSettingsStore.updateData { current ->
            current.copy(themeMode = themeMode.name)
        }
    }
}
