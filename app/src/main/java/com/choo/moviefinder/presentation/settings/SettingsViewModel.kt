package com.choo.moviefinder.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.ThemeMode
import com.choo.moviefinder.domain.usecase.ClearWatchHistoryUseCase
import com.choo.moviefinder.domain.usecase.GetThemeModeUseCase
import com.choo.moviefinder.domain.usecase.SetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getThemeModeUseCase: GetThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val clearWatchHistoryUseCase: ClearWatchHistoryUseCase
) : ViewModel() {

    val currentThemeMode: StateFlow<ThemeMode> = getThemeModeUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    private val _snackbarEvent = Channel<ErrorType>(Channel.BUFFERED)
    val snackbarEvent = _snackbarEvent.receiveAsFlow()

    private val _watchHistoryCleared = Channel<Unit>(Channel.BUFFERED)
    val watchHistoryCleared = _watchHistoryCleared.receiveAsFlow()

    // 테마 모드를 DataStore에 저장 (에러 시 Timber 로깅)
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            try {
                setThemeModeUseCase(mode)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to set theme mode to %s", mode)
            }
        }
    }

    // 시청 기록 전체 삭제 (성공 시 이벤트, 에러 시 Snackbar)
    fun clearWatchHistory() {
        viewModelScope.launch {
            try {
                clearWatchHistoryUseCase()
                _watchHistoryCleared.send(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear watch history")
                _snackbarEvent.send(ErrorMessageProvider.getErrorType(e))
            }
        }
    }
}
