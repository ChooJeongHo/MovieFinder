package com.choo.moviefinder.presentation.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.usecase.GetFavoriteMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetWatchlistUseCase
import com.choo.moviefinder.domain.usecase.ToggleFavoriteUseCase
import com.choo.moviefinder.domain.usecase.ToggleWatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    getFavoriteMoviesUseCase: GetFavoriteMoviesUseCase,
    getWatchlistUseCase: GetWatchlistUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val toggleWatchlistUseCase: ToggleWatchlistUseCase
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(FavoriteSortOrder.ADDED_DATE)
    val sortOrder: StateFlow<FavoriteSortOrder> = _sortOrder.asStateFlow()

    val favoriteMovies = combine(
        getFavoriteMoviesUseCase(),
        _sortOrder
    ) { movies, sort -> sort.apply(movies) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchlistMovies = combine(
        getWatchlistUseCase(),
        _sortOrder
    ) { movies, sort -> sort.apply(movies) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _snackbarEvent = Channel<ErrorType>(Channel.BUFFERED)
    val snackbarEvent = _snackbarEvent.receiveAsFlow()

    // 즐겨찾기 상태 토글 (에러 시 Snackbar 이벤트 전송)
    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch {
            try {
                toggleFavoriteUseCase(movie)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle favorite for movie %d", movie.id)
                _snackbarEvent.send(ErrorMessageProvider.getErrorType(e))
            }
        }
    }

    // 정렬 순서 변경
    fun onSortOrderSelected(sort: FavoriteSortOrder) {
        _sortOrder.value = sort
    }

    // 워치리스트 상태 토글 (에러 시 Snackbar 이벤트 전송)
    fun toggleWatchlist(movie: Movie) {
        viewModelScope.launch {
            try {
                toggleWatchlistUseCase(movie)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle watchlist for movie %d", movie.id)
                _snackbarEvent.send(ErrorMessageProvider.getErrorType(e))
            }
        }
    }
}
