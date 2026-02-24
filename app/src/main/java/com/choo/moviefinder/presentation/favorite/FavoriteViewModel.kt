package com.choo.moviefinder.presentation.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.usecase.GetFavoriteMoviesUseCase
import com.choo.moviefinder.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    getFavoriteMoviesUseCase: GetFavoriteMoviesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    val favoriteMovies = getFavoriteMoviesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 즐겨찾기 상태를 토글 (추가 ↔ 제거)
    // 스와이프 삭제 시 호출되고, Undo 시 다시 호출하면 재추가됨
    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch {
            try {
                toggleFavoriteUseCase(movie)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle favorite for movie %d", movie.id)
            }
        }
    }
}