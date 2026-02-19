package com.choo.moviefinder.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.usecase.GetMovieCreditsUseCase
import com.choo.moviefinder.domain.usecase.GetMovieDetailUseCase
import com.choo.moviefinder.domain.usecase.GetMovieTrailerUseCase
import com.choo.moviefinder.domain.usecase.GetSimilarMoviesUseCase
import com.choo.moviefinder.domain.usecase.IsFavoriteUseCase
import com.choo.moviefinder.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMovieDetailUseCase: GetMovieDetailUseCase,
    private val getMovieCreditsUseCase: GetMovieCreditsUseCase,
    private val getSimilarMoviesUseCase: GetSimilarMoviesUseCase,
    private val getMovieTrailerUseCase: GetMovieTrailerUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase
) : ViewModel() {

    private val movieId: Int = requireNotNull(savedStateHandle.get<Int>("movieId")) {
        "movieId argument is required for DetailViewModel"
    }

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<ErrorType>()
    val snackbarEvent: SharedFlow<ErrorType> = _snackbarEvent.asSharedFlow()

    val isFavorite = isFavoriteUseCase(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadMovieDetail()
    }

    // 상세/출연진/비슷한 영화 3개 API를 병렬 호출하여 로딩 시간 단축
    // 영화 상세는 필수 — 실패 시 Error 상태로 전환
    // 출연진/비슷한 영화는 부가 정보 — 실패해도 빈 리스트로 대체하여 상세 화면 표시
    fun loadMovieDetail() {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                coroutineScope {
                    val detailDeferred = async { getMovieDetailUseCase(movieId) }
                    val creditsDeferred = async {
                        runCatching { getMovieCreditsUseCase(movieId) }.getOrElse { emptyList() }
                    }
                    val similarDeferred = async {
                        runCatching { getSimilarMoviesUseCase(movieId) }.getOrElse { emptyList() }
                    }
                    val trailerDeferred = async {
                        runCatching { getMovieTrailerUseCase(movieId) }.getOrNull()
                    }

                    _uiState.value = DetailUiState.Success(
                        movieDetail = detailDeferred.await(),
                        credits = creditsDeferred.await(),
                        similarMovies = similarDeferred.await(),
                        trailerKey = trailerDeferred.await()
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(
                    ErrorMessageProvider.getErrorType(e)
                )
            }
        }
    }

    // MovieDetail → Movie 변환 후 toggle (Room에는 Movie 단위로 저장)
    fun toggleFavorite() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                if (state is DetailUiState.Success) {
                    val movie = Movie(
                        id = state.movieDetail.id,
                        title = state.movieDetail.title,
                        posterPath = state.movieDetail.posterPath,
                        backdropPath = state.movieDetail.backdropPath,
                        overview = state.movieDetail.overview,
                        releaseDate = state.movieDetail.releaseDate,
                        voteAverage = state.movieDetail.voteAverage,
                        voteCount = state.movieDetail.voteCount
                    )
                    toggleFavoriteUseCase(movie)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarEvent.emit(
                    ErrorMessageProvider.getErrorType(e)
                )
            }
        }
    }
}
