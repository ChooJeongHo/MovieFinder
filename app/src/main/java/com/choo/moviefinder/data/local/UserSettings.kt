package com.choo.moviefinder.data.local

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val themeMode: String = "SYSTEM",
    val monthlyWatchGoal: Int = 0,
    val lastGoalNotifiedMonth: String = "",
    val onboardingCompleted: Boolean = false,
    val tmdbAccessToken: String? = null,
    val tmdbAccountId: String? = null,
    val tmdbSessionId: String? = null,
)
