package com.choo.moviefinder.presentation.person

import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.PersonDetail

sealed class PersonDetailUiState {
    data object Loading : PersonDetailUiState()
    data class Success(
        val person: PersonDetail,
        val movies: List<Movie>
    ) : PersonDetailUiState()
    data class Error(val errorType: ErrorType) : PersonDetailUiState()
}
