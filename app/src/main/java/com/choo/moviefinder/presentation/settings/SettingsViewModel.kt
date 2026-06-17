package com.choo.moviefinder.presentation.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.core.util.WhileSubscribed5s
import com.choo.moviefinder.core.util.launchWithErrorHandler
import com.choo.moviefinder.domain.model.ThemeMode
import com.choo.moviefinder.domain.usecase.ClearWatchHistoryUseCase
import com.choo.moviefinder.domain.usecase.ExportUserDataUseCase
import com.choo.moviefinder.domain.usecase.GetMonthlyWatchGoalUseCase
import com.choo.moviefinder.domain.usecase.GetThemeModeUseCase
import com.choo.moviefinder.domain.usecase.GetTmdbAccessTokenUseCase
import com.choo.moviefinder.domain.usecase.GetTmdbRequestTokenUseCase
import com.choo.moviefinder.domain.usecase.ImportUserDataUseCase
import com.choo.moviefinder.domain.usecase.RevokeTmdbAuthUseCase
import com.choo.moviefinder.domain.usecase.SetMonthlyWatchGoalUseCase
import com.choo.moviefinder.domain.usecase.SetThemeModeUseCase
import com.choo.moviefinder.domain.usecase.SyncTmdbAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed class SyncResult {
    data class Success(val favoritesAdded: Int, val watchlistAdded: Int) : SyncResult()
    data class Failed(val message: String?) : SyncResult()
}

@HiltViewModel
@Suppress("TooManyFunctions")
class SettingsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    getThemeModeUseCase: GetThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val clearWatchHistoryUseCase: ClearWatchHistoryUseCase,
    getMonthlyWatchGoalUseCase: GetMonthlyWatchGoalUseCase,
    private val setMonthlyWatchGoalUseCase: SetMonthlyWatchGoalUseCase,
    private val exportUserDataUseCase: ExportUserDataUseCase,
    private val importUserDataUseCase: ImportUserDataUseCase,
    getTmdbAccessTokenUseCase: GetTmdbAccessTokenUseCase,
    private val getTmdbRequestTokenUseCase: GetTmdbRequestTokenUseCase,
    private val revokeTmdbAuthUseCase: RevokeTmdbAuthUseCase,
    private val syncTmdbAccountUseCase: SyncTmdbAccountUseCase,
) : ViewModel() {

    val currentThemeMode: StateFlow<ThemeMode> = getThemeModeUseCase()
        .stateIn(viewModelScope, WhileSubscribed5s, ThemeMode.SYSTEM)

    // 0 = 목표 없음
    val monthlyWatchGoal: StateFlow<Int> = getMonthlyWatchGoalUseCase()
        .stateIn(viewModelScope, WhileSubscribed5s, 0)

    // null = 미연결
    val tmdbAccessToken: StateFlow<String?> = getTmdbAccessTokenUseCase()
        .stateIn(viewModelScope, WhileSubscribed5s, null)

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    private val _snackbarEvent = Channel<ErrorType>(Channel.CONFLATED)
    val snackbarEvent = _snackbarEvent.receiveAsFlow()

    private val _watchHistoryCleared = Channel<Unit>(Channel.CONFLATED)
    val watchHistoryCleared = _watchHistoryCleared.receiveAsFlow()

    private val _exportedJson = Channel<String>(Channel.CONFLATED)
    val exportedJson = _exportedJson.receiveAsFlow()

    private val _importSuccess = Channel<Int>(Channel.CONFLATED)
    val importSuccess = _importSuccess.receiveAsFlow()

    private val _openTmdbAuth = Channel<String>(Channel.CONFLATED)
    val openTmdbAuth = _openTmdbAuth.receiveAsFlow()

    private val _syncResult = Channel<SyncResult>(Channel.CONFLATED)
    val syncResult = _syncResult.receiveAsFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _disconnectSuccess = Channel<Unit>(Channel.CONFLATED)
    val disconnectSuccess = _disconnectSuccess.receiveAsFlow()

    // TransactionTooLargeException 방지로 SavedStateHandle 미사용
    @Volatile
    var pendingExportJson: String? = null

    // OAuth CSRF 방지: startTmdbAuth()에서 발급한 요청 토큰을 콜백 검증 전까지 보관
    // SavedStateHandle로 프로세스 종료 후에도 복원 (짧은 문자열이라 TransactionTooLargeException 위험 없음)
    var pendingRequestToken: String?
        get() = savedStateHandle["pendingRequestToken"]
        private set(value) {
            savedStateHandle["pendingRequestToken"] = value
        }

    fun clearPendingToken() {
        pendingRequestToken = null
    }

    // DataStore에 저장
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launchWithErrorHandler(
        onError = {
            Timber.e("테마 모드를 %s로 설정 실패", mode)
            _snackbarEvent.trySend(it)
        }
    ) {
        setThemeModeUseCase(mode)
    }

    // DataStore에 저장
    fun setMonthlyWatchGoal(goal: Int) = viewModelScope.launchWithErrorHandler(
        onError = {
            Timber.e("월간 시청 목표를 %d로 설정 실패", goal)
            _snackbarEvent.trySend(it)
        }
    ) {
        setMonthlyWatchGoalUseCase(goal)
    }

    // JSON 문자열로 내보냄 (성공 시 exportedJson 이벤트)
    fun exportData() = viewModelScope.launchWithErrorHandler(
        onError = {
            Timber.e("사용자 데이터 내보내기 실패")
            _snackbarEvent.trySend(it)
        }
    ) {
        _exportedJson.trySend(exportUserDataUseCase())
    }

    // 성공 시 importSuccess 이벤트
    fun importData(jsonString: String) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val count = importUserDataUseCase(jsonString)
                _importSuccess.trySend(count)
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

    // 성공 시 watchHistoryCleared 이벤트
    fun clearWatchHistory() = viewModelScope.launchWithErrorHandler(
        onError = {
            Timber.e("시청 기록 삭제 실패")
            _snackbarEvent.trySend(it)
        }
    ) {
        clearWatchHistoryUseCase()
        _watchHistoryCleared.trySend(Unit)
    }

    // TMDB v4 요청 토큰 발급 후 Chrome Custom Tab으로 인증 URL 오픈
    fun startTmdbAuth() = viewModelScope.launchWithErrorHandler(
        onError = {
            Timber.e("TMDB 요청 토큰 발급 실패")
            _snackbarEvent.trySend(it)
        }
    ) {
        val requestToken = getTmdbRequestTokenUseCase()
        pendingRequestToken = requestToken
        val authUrl = "https://www.themoviedb.org/auth/access" +
            "?request_token=$requestToken" +
            "&redirect_to=moviefinder://auth/callback"
        _openTmdbAuth.trySend(authUrl)
    }

    fun disconnectTmdb() {
        val token = tmdbAccessToken.value ?: return
        viewModelScope.launchWithErrorHandler(
            onError = {
                Timber.e("TMDB 계정 연결 해제 실패")
                _snackbarEvent.trySend(it)
            }
        ) {
            revokeTmdbAuthUseCase(token)
            _disconnectSuccess.trySend(Unit)
        }
    }

    // 즐겨찾기·워치리스트를 로컬 DB에 동기화
    // _isSyncing finally 보장이 필요해 launchWithErrorHandler 대신 수동 try-catch 유지
    fun syncTmdbAccount() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = syncTmdbAccountUseCase()
                _syncResult.trySend(SyncResult.Success(result.favoritesAdded, result.watchlistAdded))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "TMDB 동기화 실패")
                _syncResult.trySend(SyncResult.Failed(e.message))
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
