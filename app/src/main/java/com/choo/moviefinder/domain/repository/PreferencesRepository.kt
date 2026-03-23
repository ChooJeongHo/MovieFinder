package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
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