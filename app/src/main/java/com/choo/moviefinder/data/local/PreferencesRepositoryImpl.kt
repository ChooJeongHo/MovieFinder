package com.choo.moviefinder.data.local

import androidx.datastore.core.DataStore
import com.choo.moviefinder.domain.model.ThemeMode
import com.choo.moviefinder.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class PreferencesRepositoryImpl @Inject constructor(
    private val userSettingsStore: DataStore<UserSettings>,
    private val secureTokenStore: SecureTokenStore
) : PreferencesRepository {

    private val _tmdbAccessToken = MutableStateFlow(secureTokenStore.getAccessToken())

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

    // 월간 시청 목표 편수를 Flow로 조회
    override fun getMonthlyWatchGoal(): Flow<Int> {
        return userSettingsStore.data.map { it.monthlyWatchGoal }
    }

    // 월간 시청 목표 편수를 DataStore에 저장
    override suspend fun setMonthlyWatchGoal(goal: Int) {
        userSettingsStore.updateData { current ->
            current.copy(monthlyWatchGoal = goal)
        }
    }

    // 마지막 목표 달성 알림 월을 Flow로 조회
    override fun getLastGoalNotifiedMonth(): Flow<String> {
        return userSettingsStore.data.map { it.lastGoalNotifiedMonth }
    }

    // 마지막 목표 달성 알림 월을 DataStore에 저장
    override suspend fun setLastGoalNotifiedMonth(yearMonth: String) {
        userSettingsStore.updateData { current ->
            current.copy(lastGoalNotifiedMonth = yearMonth)
        }
    }

    // TMDB 액세스 토큰을 Flow로 조회 (EncryptedSharedPreferences 기반 MutableStateFlow)
    override fun getTmdbAccessToken(): Flow<String?> = _tmdbAccessToken.asStateFlow()

    // TMDB 인증 정보(액세스 토큰, 계정 ID, 세션 ID)를 EncryptedSharedPreferences에 저장
    override suspend fun saveTmdbAuth(accessToken: String, accountId: String, sessionId: String) {
        secureTokenStore.saveTokens(accessToken, accountId, sessionId)
        _tmdbAccessToken.value = accessToken
    }

    // TMDB 인증 정보를 EncryptedSharedPreferences에서 삭제
    override suspend fun clearTmdbAuth() {
        secureTokenStore.clearTokens()
        _tmdbAccessToken.value = null
    }

    // TMDB 세션 ID를 일회성으로 조회
    override suspend fun getTmdbSessionIdOnce(): String? = secureTokenStore.getSessionId()

    // TMDB 액세스 토큰과 계정 ID를 일회성으로 조회
    override suspend fun getTmdbAuthOnce(): Pair<String?, String?> =
        Pair(secureTokenStore.getAccessToken(), secureTokenStore.getAccountId())

    // 온보딩 완료 상태를 DataStore에 저장
    override suspend fun setOnboardingCompleted() {
        userSettingsStore.updateData { current ->
            current.copy(onboardingCompleted = true)
        }
    }
}
