package com.choo.moviefinder.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.domain.usecase.GetWatchStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    getWatchStatsUseCase: GetWatchStatsUseCase
) : ViewModel() {

    // 시청 통계 데이터를 stateIn으로 수집하여 UI 상태를 자동 갱신한다
    val uiState: StateFlow<StatsUiState> = getWatchStatsUseCase()
        .map<_, StatsUiState> { StatsUiState.Success(it) }
        .catch { e ->
            if (e is CancellationException) throw e
            emit(StatsUiState.Error(ErrorMessageProvider.getErrorType(e)))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState.Loading)
}
