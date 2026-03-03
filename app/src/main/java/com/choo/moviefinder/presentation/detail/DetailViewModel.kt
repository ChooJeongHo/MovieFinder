package com.choo.moviefinder.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.usecase.GetMovieCertificationUseCase
import com.choo.moviefinder.domain.usecase.GetMovieCreditsUseCase
import com.choo.moviefinder.domain.usecase.GetMovieDetailUseCase
import com.choo.moviefinder.domain.usecase.GetMovieTrailerUseCase
import com.choo.moviefinder.domain.usecase.GetSimilarMoviesUseCase
import com.choo.moviefinder.domain.usecase.IsFavoriteUseCase
import com.choo.moviefinder.domain.usecase.IsInWatchlistUseCase
import com.choo.moviefinder.domain.usecase.SaveWatchHistoryUseCase
import com.choo.moviefinder.domain.usecase.ToggleFavoriteUseCase
import com.choo.moviefinder.domain.usecase.ToggleWatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMovieDetailUseCase: GetMovieDetailUseCase,
    private val getMovieCreditsUseCase: GetMovieCreditsUseCase,
    private val getSimilarMoviesUseCase: GetSimilarMoviesUseCase,
    private val getMovieTrailerUseCase: GetMovieTrailerUseCase,
    private val getMovieCertificationUseCase: GetMovieCertificationUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase,
    private val toggleWatchlistUseCase: ToggleWatchlistUseCase,
    private val isInWatchlistUseCase: IsInWatchlistUseCase,
    private val saveWatchHistoryUseCase: SaveWatchHistoryUseCase
) : ViewModel() {

    private val movieId: Int = requireNotNull(savedStateHandle.get<Int>("movieId")) {
        "movieId argument is required for DetailViewModel"
    }

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _snackbarEvent = Channel<ErrorType>(Channel.BUFFERED)
    val snackbarEvent = _snackbarEvent.receiveAsFlow()

    private var isLoading = false

    val isFavorite = isFavoriteUseCase(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isInWatchlist = isInWatchlistUseCase(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadMovieDetail()
    }

    fun loadMovieDetail() {
        if (isLoading) return
        isLoading = true
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                coroutineScope {
                    val detailDeferred = async { getMovieDetailUseCase(movieId) }
                    val creditsDeferred = async {
                        runCatching { getMovieCreditsUseCase(movieId) }
                            .onFailure { Timber.w(it, "Failed to load credits for movie %d", movieId) }
                            .getOrElse { emptyList() }
                    }
                    val similarDeferred = async {
                        runCatching { getSimilarMoviesUseCase(movieId) }
                            .onFailure { Timber.w(it, "Failed to load similar movies for movie %d", movieId) }
                            .getOrElse { emptyList() }
                    }
                    val trailerDeferred = async {
                        runCatching { getMovieTrailerUseCase(movieId) }
                            .onFailure { Timber.w(it, "Failed to load trailer for movie %d", movieId) }
                            .getOrNull()
                    }
                    val certificationDeferred = async {
                        runCatching { getMovieCertificationUseCase(movieId) }
                            .onFailure { Timber.w(it, "Failed to load certification for movie %d", movieId) }
                            .getOrNull()
                    }

                    val detail = detailDeferred.await()
                    _uiState.value = DetailUiState.Success(
                        movieDetail = detail,
                        credits = creditsDeferred.await(),
                        similarMovies = similarDeferred.await(),
                        trailerKey = trailerDeferred.await(),
                        certification = certificationDeferred.await()
                    )

                    // 시청 기록 저장
                    val movie = Movie(
                        id = detail.id,
                        title = detail.title,
                        posterPath = detail.posterPath,
                        backdropPath = detail.backdropPath,
                        overview = detail.overview,
                        releaseDate = detail.releaseDate,
                        voteAverage = detail.voteAverage,
                        voteCount = detail.voteCount
                    )
                    runCatching { saveWatchHistoryUseCase(movie) }
                        .onFailure { Timber.w(it, "Failed to save watch history for movie %d", movieId) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(
                    ErrorMessageProvider.getErrorType(e)
                )
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                if (state is DetailUiState.Success) {
                    val movie = state.movieDetail.toMovie()
                    toggleFavoriteUseCase(movie)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarEvent.send(
                    ErrorMessageProvider.getErrorType(e)
                )
            }
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                if (state is DetailUiState.Success) {
                    val movie = state.movieDetail.toMovie()
                    toggleWatchlistUseCase(movie)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarEvent.send(
                    ErrorMessageProvider.getErrorType(e)
                )
            }
        }
    }

    private fun com.choo.moviefinder.domain.model.MovieDetail.toMovie() = Movie(
        id = id,
        title = title,
        posterPath = posterPath,
        backdropPath = backdropPath,
        overview = overview,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        voteCount = voteCount
    )
}
