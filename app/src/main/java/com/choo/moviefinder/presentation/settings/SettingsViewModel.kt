package com.choo.moviefinder.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.domain.model.ThemeMode
import com.choo.moviefinder.domain.usecase.ClearWatchHistoryUseCase
import com.choo.moviefinder.domain.usecase.GetThemeModeUseCase
import com.choo.moviefinder.domain.usecase.SetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getThemeModeUseCase: GetThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val clearWatchHistoryUseCase: ClearWatchHistoryUseCase
) : ViewModel() {

    val currentThemeMode: StateFlow<ThemeMode> = getThemeModeUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            setThemeModeUseCase(mode)
        }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            clearWatchHistoryUseCase()
        }
    }
}
