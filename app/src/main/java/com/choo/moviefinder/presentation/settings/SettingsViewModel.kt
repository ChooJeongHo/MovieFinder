package com.choo.moviefinder.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.ThemeMode
import com.choo.moviefinder.domain.model.UserDataBackup
import com.choo.moviefinder.domain.usecase.ClearWatchHistoryUseCase
import com.choo.moviefinder.domain.usecase.ExportUserDataUseCase
import com.choo.moviefinder.domain.usecase.GetMonthlyWatchGoalUseCase
import com.choo.moviefinder.domain.usecase.GetThemeModeUseCase
import com.choo.moviefinder.domain.usecase.ImportUserDataUseCase
import com.choo.moviefinder.domain.usecase.SetMonthlyWatchGoalUseCase
import com.choo.moviefinder.domain.usecase.SetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getThemeModeUseCase: GetThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val clearWatchHistoryUseCase: ClearWatchHistoryUseCase,
    getMonthlyWatchGoalUseCase: GetMonthlyWatchGoalUseCase,
    private val setMonthlyWatchGoalUseCase: SetMonthlyWatchGoalUseCase,
    private val exportUserDataUseCase: ExportUserDataUseCase,
    private val importUserDataUseCase: ImportUserDataUseCase
) : ViewModel() {

    val currentThemeMode: StateFlow<ThemeMode> = getThemeModeUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    // 이번 달 시청 목표 편수 Flow (0 = 목표 없음)
    val monthlyWatchGoal: StateFlow<Int> = getMonthlyWatchGoalUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _snackbarEvent = Channel<ErrorType>(Channel.CONFLATED)
    val snackbarEvent = _snackbarEvent.receiveAsFlow()

    private val _watchHistoryCleared = Channel<Unit>(Channel.CONFLATED)
    val watchHistoryCleared = _watchHistoryCleared.receiveAsFlow()

    private val _exportedJson = Channel<String>(Channel.CONFLATED)
    val exportedJson = _exportedJson.receiveAsFlow()

    private val _importSuccess = Channel<Unit>(Channel.CONFLATED)
    val importSuccess = _importSuccess.receiveAsFlow()

    // 파일 저장 런처가 실행되기 전까지 JSON을 임시 보관한다
    var pendingExportJson: String? = null
        private set

    fun setPendingExportJson(json: String) {
        pendingExportJson = json
    }

    fun clearPendingExportJson() {
        pendingExportJson = null
    }

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

    // 이번 달 시청 목표 편수 저장 (에러 시 Timber 로깅)
    fun setMonthlyWatchGoal(goal: Int) {
        viewModelScope.launch {
            try {
                setMonthlyWatchGoalUseCase(goal)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to set monthly watch goal to %d", goal)
            }
        }
    }

    // 사용자 데이터를 JSON 문자열로 내보낸다 (성공 시 JSON 이벤트, 에러 시 Snackbar)
    fun exportData() {
        viewModelScope.launch {
            try {
                val backup = exportUserDataUseCase()
                val json = Json.encodeToString(backup)
                _exportedJson.send(json)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to export user data")
                _snackbarEvent.send(ErrorMessageProvider.getErrorType(e))
            }
        }
    }

    // JSON 문자열에서 사용자 데이터를 가져온다 (성공 시 이벤트, 에러 시 Snackbar)
    fun importData(jsonString: String) {
        viewModelScope.launch {
            try {
                val backup = Json.decodeFromString<UserDataBackup>(jsonString)
                importUserDataUseCase(backup)
                _importSuccess.send(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to import user data")
                _snackbarEvent.send(ErrorMessageProvider.getErrorType(e))
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
