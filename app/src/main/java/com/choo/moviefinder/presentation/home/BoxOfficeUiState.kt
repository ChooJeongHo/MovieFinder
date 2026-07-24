package com.choo.moviefinder.presentation.home

import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.BoxOfficeMovie

sealed class BoxOfficeUiState {
    data object Loading : BoxOfficeUiState()
    data class Success(val items: List<BoxOfficeMovie>) : BoxOfficeUiState()
    data class Error(val errorType: ErrorType) : BoxOfficeUiState()
}
