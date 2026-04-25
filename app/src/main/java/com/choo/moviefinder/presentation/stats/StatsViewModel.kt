package com.choo.moviefinder.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.domain.usecase.GetWatchStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val getWatchStatsUseCase: GetWatchStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        loadJob?.cancel()
        // Set Loading BEFORE launching the new coroutine so any final emission from the
        // cancelled job (cooperative cancellation is not instantaneous) cannot overwrite
        // the Loading state that the new job intends as its starting point.
        _uiState.value = StatsUiState.Loading
        loadJob = viewModelScope.launch {
            getWatchStatsUseCase()
                .map<_, StatsUiState> { StatsUiState.Success(it) }
                .catch { e ->
                    if (e is CancellationException) throw e
                    emit(StatsUiState.Error(ErrorMessageProvider.getErrorType(e)))
                }
                .collect { _uiState.value = it }
        }
    }
}
