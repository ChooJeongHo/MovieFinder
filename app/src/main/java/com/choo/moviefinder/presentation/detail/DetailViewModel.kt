package com.choo.moviefinder.presentation.detail

// Detail screen ViewModel - manages movie detail UI state and user actions
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.core.notification.ReleaseNotificationScheduler
import com.choo.moviefinder.core.notification.WatchGoalNotificationHelper
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.MovieDetail
import com.choo.moviefinder.domain.usecase.DeleteMemoUseCase
import com.choo.moviefinder.domain.usecase.GetMemosUseCase
import com.choo.moviefinder.domain.usecase.GetMovieCertificationUseCase
import com.choo.moviefinder.domain.usecase.DeleteUserRatingUseCase
import com.choo.moviefinder.domain.usecase.GetMovieRecommendationsUseCase
import com.choo.moviefinder.domain.usecase.SaveMemoUseCase
import com.choo.moviefinder.domain.usecase.UpdateMemoUseCase
import com.choo.moviefinder.domain.usecase.GetMovieCreditsUseCase
import com.choo.moviefinder.domain.usecase.GetMovieDetailUseCase
import com.choo.moviefinder.domain.usecase.GetMovieReviewsUseCase
import com.choo.moviefinder.domain.usecase.GetMovieTrailerUseCase
import com.choo.moviefinder.domain.usecase.GetSimilarMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetUserRatingUseCase
import com.choo.moviefinder.domain.usecase.IsFavoriteUseCase
import com.choo.moviefinder.domain.usecase.IsInWatchlistUseCase
import com.choo.moviefinder.domain.usecase.SaveWatchHistoryUseCase
import com.choo.moviefinder.domain.usecase.SetUserRatingUseCase
import com.choo.moviefinder.domain.usecase.ToggleFavoriteUseCase
import com.choo.moviefinder.domain.usecase.ToggleWatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val getMovieReviewsUseCase: GetMovieReviewsUseCase,
    private val getMovieRecommendationsUseCase: GetMovieRecommendationsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase,
    private val toggleWatchlistUseCase: ToggleWatchlistUseCase,
    private val isInWatchlistUseCase: IsInWatchlistUseCase,
    private val saveWatchHistoryUseCase: SaveWatchHistoryUseCase,
    private val getUserRatingUseCase: GetUserRatingUseCase,
    private val setUserRatingUseCase: SetUserRatingUseCase,
    private val deleteUserRatingUseCase: DeleteUserRatingUseCase,
    private val getMemosUseCase: GetMemosUseCase,
    private val saveMemoUseCase: SaveMemoUseCase,
    private val updateMemoUseCase: UpdateMemoUseCase,
    private val deleteMemoUseCase: DeleteMemoUseCase,
    private val releaseNotificationScheduler: ReleaseNotificationScheduler,
    private val watchGoalNotificationHelper: WatchGoalNotificationHelper
) : ViewModel() {

    private val movieId: Int = requireNotNull(savedStateHandle.get<Int>("movieId")) {
        "movieId argument is required for DetailViewModel"
    }

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _snackbarEvent = Channel<ErrorType>(Channel.CONFLATED)
    val snackbarEvent = _snackbarEvent.receiveAsFlow()

    private val loadingMutex = Mutex()
    private val toggleMutex = Mutex()

    val isFavorite = isFavoriteUseCase(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isInWatchlist = isInWatchlistUseCase(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userRating = getUserRatingUseCase(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        loadMovieDetail()
    }

    // 영화 상세/출연진/비슷한 영화/리뷰/예고편/등급 6개 API 병렬 호출
    fun loadMovieDetail() {
        viewModelScope.launch {
            if (!loadingMutex.tryLock()) return@launch
            _uiState.value = DetailUiState.Loading
            try {
                val detail: MovieDetail
                coroutineScope {
                    val detailDeferred = async { getMovieDetailUseCase(movieId) }
                    val creditsDeferred = async {
                        loadOptional("credits") { getMovieCreditsUseCase(movieId) }
                    }
                    val similarDeferred = async {
                        loadOptional("similar") { getSimilarMoviesUseCase(movieId) }
                    }
                    val reviewsDeferred = async {
                        loadOptional("reviews") { getMovieReviewsUseCase(movieId) }
                    }
                    val trailerDeferred = async {
                        loadOptionalNullable("trailer") { getMovieTrailerUseCase(movieId) }
                    }
                    val certDeferred = async {
                        loadOptionalNullable("cert") { getMovieCertificationUseCase(movieId) }
                    }
                    val recommendationsDeferred = async {
                        loadOptional("recommendations") { getMovieRecommendationsUseCase(movieId) }
                    }

                    detail = detailDeferred.await()
                    _uiState.value = DetailUiState.Success(
                        movieDetail = detail,
                        credits = creditsDeferred.await(),
                        similarMovies = similarDeferred.await(),
                        trailerKey = trailerDeferred.await(),
                        certification = certDeferred.await(),
                        reviews = reviewsDeferred.await(),
                        recommendations = recommendationsDeferred.await()
                    )
                }
                saveWatchHistory(detail)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(ErrorMessageProvider.getErrorType(e))
            } finally {
                loadingMutex.unlock()
            }
        }
    }

    // 부분 실패 허용 로드 (실패 시 빈 리스트 반환)
    private suspend fun <T> loadOptional(tag: String, block: suspend () -> List<T>): List<T> =
        runCatching { block() }
            .onFailure { Timber.w(it, "Failed to load %s for movie %d", tag, movieId) }
            .getOrElse { emptyList() }

    // 부분 실패 허용 로드 (실패 시 null 반환)
    private suspend fun <T> loadOptionalNullable(tag: String, block: suspend () -> T?): T? =
        runCatching { block() }
            .onFailure { Timber.w(it, "Failed to load %s for movie %d", tag, movieId) }
            .getOrNull()

    // 영화 상세 화면 진입 시 장르 정보와 함께 시청 기록을 Room DB에 저장하고 목표 달성을 확인한다
    private suspend fun saveWatchHistory(detail: MovieDetail) {
        val movie = detail.toMovie()
        val genres = detail.genres.joinToString(",") { it.name }
        runCatching { saveWatchHistoryUseCase(movie, genres) }
            .onFailure { Timber.w(it, "Failed to save watch history for movie %d", movieId) }
        runCatching { watchGoalNotificationHelper.checkAndNotifyGoalAchieved() }
            .onFailure { Timber.w(it, "Failed to check watch goal for movie %d", movieId) }
    }

    // 즐겨찾기 상태 토글 (에러 시 Snackbar 이벤트 전송)
    fun toggleFavorite() = launchWithSnackbar {
        toggleMutex.withLock {
            val state = _uiState.value
            if (state is DetailUiState.Success) {
                toggleFavoriteUseCase(state.movieDetail.toMovie())
            }
        }
    }

    // 워치리스트 토글 및 개봉일 알림 예약/취소
    fun toggleWatchlist() = launchWithSnackbar {
        toggleMutex.withLock {
            val state = _uiState.value
            if (state is DetailUiState.Success) {
                val wasInWatchlist = isInWatchlist.value
                val movie = state.movieDetail.toMovie()
                toggleWatchlistUseCase(movie)
                if (!wasInWatchlist) {
                    val detail = state.movieDetail
                    releaseNotificationScheduler.schedule(
                        movieId = detail.id,
                        movieTitle = detail.title,
                        releaseDate = detail.releaseDate
                    )
                } else {
                    releaseNotificationScheduler.cancel(movieId)
                }
            }
        }
    }

    // 사용자 영화 평점을 Room DB에 저장
    fun setUserRating(rating: Float) = launchWithSnackbar {
        setUserRatingUseCase(movieId, rating)
        Timber.d("User rating set to %.1f for movie %d", rating, movieId)
    }

    // 사용자 영화 평점을 Room DB에서 삭제
    fun deleteUserRating() = launchWithSnackbar {
        deleteUserRatingUseCase(movieId)
        Timber.d("User rating deleted for movie %d", movieId)
    }

    val memos = getMemosUseCase(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 영화에 새 메모를 저장
    fun saveMemo(content: String) = launchWithSnackbar {
        saveMemoUseCase(movieId, content)
    }

    // 기존 메모 내용을 수정
    fun updateMemo(memoId: Long, content: String) = launchWithSnackbar {
        updateMemoUseCase(memoId, content)
    }

    // 메모를 삭제
    fun deleteMemo(memoId: Long) = launchWithSnackbar {
        deleteMemoUseCase(memoId)
    }

    // 코루틴 실행 후 예외 발생 시 Snackbar 에러 이벤트 전송
    private fun launchWithSnackbar(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarEvent.send(ErrorMessageProvider.getErrorType(e))
            }
        }
    }
}