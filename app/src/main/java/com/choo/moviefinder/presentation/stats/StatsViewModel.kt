package com.choo.moviefinder.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.core.util.ErrorMessageProvider
import kotlinx.coroutines.flow.SharingStarted
import com.choo.moviefinder.domain.usecase.GetWatchStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val getWatchStatsUseCase: GetWatchStatsUseCase
) : ViewModel() {

    private val _retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<StatsUiState> = _retryTrigger
        .flatMapLatest {
            getWatchStatsUseCase()
                .map<_, StatsUiState> { StatsUiState.Success(it) }
                .onStart { emit(StatsUiState.Loading) }
                .catch { e ->
                    if (e is CancellationException) throw e
                    emit(StatsUiState.Error(ErrorMessageProvider.getErrorType(e)))
                }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, StatsUiState.Loading)

    fun retry() {
        _retryTrigger.value++
    }
}
