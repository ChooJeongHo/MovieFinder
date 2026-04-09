package com.choo.moviefinder.presentation.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.core.util.launchWithErrorHandler
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
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val savedStateHandle: SavedStateHandle,
    getThemeModeUseCase: GetThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val clearWatchHistoryUseCase: ClearWatchHistoryUseCase,
    getMonthlyWatchGoalUseCase: GetMonthlyWatchGoalUseCase,
    private val setMonthlyWatchGoalUseCase: SetMonthlyWatchGoalUseCase,
    private val exportUserDataUseCase: ExportUserDataUseCase,
    private val importUserDataUseCase: ImportUserDataUseCase,
    private val json: Json
) : ViewModel() {

    val currentThemeMode: StateFlow<ThemeMode> = getThemeModeUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    // 이번 달 시청 목표 편수 Flow (0 = 목표 없음)
    val monthlyWatchGoal: StateFlow<Int> = getMonthlyWatchGoalUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    private val _snackbarEvent = Channel<ErrorType>(Channel.CONFLATED)
    val snackbarEvent = _snackbarEvent.receiveAsFlow()

    private val _watchHistoryCleared = Channel<Unit>(Channel.CONFLATED)
    val watchHistoryCleared = _watchHistoryCleared.receiveAsFlow()

    private val _exportedJson = Channel<String>(Channel.CONFLATED)
    val exportedJson = _exportedJson.receiveAsFlow()

    private val _importSuccess = Channel<Unit>(Channel.CONFLATED)
    val importSuccess = _importSuccess.receiveAsFlow()

    // 파일 저장 런처가 실행되기 전까지 JSON을 임시 보관한다 (프로세스 사망 시 복원 가능)
    var pendingExportJson: String?
        get() = savedStateHandle.get<String>("pendingExportJson")
        set(value) {
            savedStateHandle["pendingExportJson"] = value
        }

    // 테마 모드를 DataStore에 저장 (에러 시 Snackbar 피드백)
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launchWithErrorHandler(
        onError = {
            Timber.e("테마 모드를 %s로 설정 실패", mode)
            _snackbarEvent.trySend(it)
        }
    ) {
        setThemeModeUseCase(mode)
    }

    // 이번 달 시청 목표 편수 저장 (에러 시 Snackbar 피드백)
    fun setMonthlyWatchGoal(goal: Int) = viewModelScope.launchWithErrorHandler(
        onError = {
            Timber.e("월간 시청 목표를 %d로 설정 실패", goal)
            _snackbarEvent.trySend(it)
        }
    ) {
        setMonthlyWatchGoalUseCase(goal)
    }

    // 사용자 데이터를 JSON 문자열로 내보낸다 (성공 시 JSON 이벤트, 에러 시 Snackbar)
    fun exportData() = viewModelScope.launchWithErrorHandler(
        onError = {
            Timber.e("사용자 데이터 내보내기 실패")
            _snackbarEvent.trySend(it)
        }
    ) {
        val backup = exportUserDataUseCase()
        val jsonString = json.encodeToString(backup)
        _exportedJson.trySend(jsonString)
    }

    // JSON 문자열에서 사용자 데이터를 가져온다 (성공 시 이벤트, 에러 시 Snackbar)
    fun importData(jsonString: String) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val backup = json.decodeFromString<UserDataBackup>(jsonString)
                importUserDataUseCase(backup)
                _importSuccess.trySend(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "사용자 데이터 가져오기 실패")
                _snackbarEvent.trySend(ErrorMessageProvider.getErrorType(e))
            } finally {
                _isImporting.value = false
            }
        }
    }

    // 시청 기록 전체 삭제 (성공 시 이벤트, 에러 시 Snackbar)
    fun clearWatchHistory() = viewModelScope.launchWithErrorHandler(
        onError = {
            Timber.e("시청 기록 삭제 실패")
            _snackbarEvent.trySend(it)
        }
    ) {
        clearWatchHistoryUseCase()
        _watchHistoryCleared.trySend(Unit)
    }
}
