package com.choo.moviefinder.presentation.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.usecase.GetFavoriteMoviesUseCase
import com.choo.moviefinder.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    getFavoriteMoviesUseCase: GetFavoriteMoviesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    val favoriteMovies = getFavoriteMoviesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeFavorite(movie: Movie) {
        viewModelScope.launch {
            toggleFavoriteUseCase(movie)
        }
    }
}