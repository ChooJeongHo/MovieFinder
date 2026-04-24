package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {

    // TMDB 액세스 토큰을 Flow로 반환한다
    fun getTmdbAccessToken(): Flow<String?>

    // TMDB 인증 정보(액세스 토큰, 계정 ID, 세션 ID)를 저장한다
    suspend fun saveTmdbAuth(accessToken: String, accountId: String, sessionId: String)

    // TMDB 인증 정보를 삭제한다
    suspend fun clearTmdbAuth()

    // TMDB 세션 ID를 일회성으로 조회한다
    suspend fun getTmdbSessionIdOnce(): String?

    // TMDB 액세스 토큰과 계정 ID를 일회성으로 조회한다 (Pair<accessToken, accountId>)
    suspend fun getTmdbAuthOnce(): Pair<String?, String?>

    // 현재 테마 모드 설정을 Flow로 반환한다
    fun getThemeMode(): Flow<ThemeMode>

    // 테마 모드를 변경하여 저장한다
    suspend fun setThemeMode(themeMode: ThemeMode)

    // 월간 시청 목표 편수를 Flow로 반환한다
    fun getMonthlyWatchGoal(): Flow<Int>

    // 월간 시청 목표 편수를 저장한다
    suspend fun setMonthlyWatchGoal(goal: Int)

    // 마지막으로 목표 달성 알림을 보낸 월을 Flow로 반환한다
    fun getLastGoalNotifiedMonth(): Flow<String>

    // 마지막으로 목표 달성 알림을 보낸 월을 저장한다
    suspend fun setLastGoalNotifiedMonth(yearMonth: String)
}