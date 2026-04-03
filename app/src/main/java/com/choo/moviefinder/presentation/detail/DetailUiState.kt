package com.choo.moviefinder.presentation.detail

import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.Cast
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.MovieDetail
import com.choo.moviefinder.domain.model.Review

sealed class DetailUiState {
    data object Loading : DetailUiState()
    data class Success(
        val movieDetail: MovieDetail,
        val credits: List<Cast>? = null,
        val similarMovies: List<Movie>? = null,
        val trailerKey: String? = null,
        val certification: String? = null,
        val reviews: List<Review>? = null,
        val recommendations: List<Movie>? = null
    ) : DetailUiState()
    data class Error(val errorType: ErrorType) : DetailUiState()
}