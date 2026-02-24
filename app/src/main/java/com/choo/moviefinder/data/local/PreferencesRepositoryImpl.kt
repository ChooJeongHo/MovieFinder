package com.choo.moviefinder.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.choo.moviefinder.domain.model.ThemeMode
import com.choo.moviefinder.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    override fun getThemeMode(): Flow<ThemeMode> {
        return dataStore.data.map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            try {
                ThemeMode.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                Timber.w(e, "잘못된 테마 설정값: %s, 기본값(SYSTEM)으로 복구", themeName)
                ThemeMode.SYSTEM
            }
        }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }
}