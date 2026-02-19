package com.choo.moviefinder.presentation.detail

import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.Cast
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.MovieDetail

sealed class DetailUiState {
    data object Loading : DetailUiState()
    data class Success(
        val movieDetail: MovieDetail,
        val credits: List<Cast>,
        val similarMovies: List<Movie>,
        val trailerKey: String? = null
    ) : DetailUiState()
    data class Error(val errorType: ErrorType) : DetailUiState()
}