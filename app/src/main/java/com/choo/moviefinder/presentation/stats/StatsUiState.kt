package com.choo.moviefinder.presentation.stats

import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.WatchStats

sealed class StatsUiState {
    data object Loading : StatsUiState()
    data class Success(val stats: WatchStats) : StatsUiState()
    data class Error(val errorType: ErrorType) : StatsUiState()
}
