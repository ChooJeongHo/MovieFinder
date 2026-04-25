package com.choo.moviefinder.presentation.detail

import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.Credits
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.MovieDetail
import com.choo.moviefinder.domain.model.Review
import com.choo.moviefinder.domain.model.WatchProvider

sealed class DetailUiState {
    data object Loading : DetailUiState()
    data class Success(
        val movieDetail: MovieDetail,
        val credits: Credits? = null,
        val similarMovies: List<Movie>? = null,
        val trailerKey: String? = null,
        val certification: String? = null,
        val reviews: List<Review>? = null,
        val recommendations: List<Movie>? = null,
        val watchProviders: List<WatchProvider>? = null
    ) : DetailUiState()
    data class Error(val errorType: ErrorType) : DetailUiState()
}