package com.choo.moviefinder.presentation.detail

// Detail screen ViewModel - manages movie detail UI state and user actions
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.core.notification.ReleaseNotificationScheduler
import com.choo.moviefinder.core.notification.WatchGoalNotificationHelper
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.core.util.WhileSubscribed5s
import com.choo.moviefinder.core.util.launchWithErrorHandler
import com.choo.moviefinder.core.util.suspendRunCatching
import com.choo.moviefinder.domain.model.MovieDetail
import com.choo.moviefinder.domain.usecase.DeleteMemoUseCase
import com.choo.moviefinder.domain.usecase.GetTmdbAccessTokenUseCase
import com.choo.moviefinder.domain.usecase.SubmitTmdbRatingUseCase
import com.choo.moviefinder.domain.usecase.DeleteUserRatingUseCase
import com.choo.moviefinder.domain.usecase.GetMemosUseCase
import com.choo.moviefinder.domain.usecase.GetUserRatingUseCase
import com.choo.moviefinder.domain.usecase.IsFavoriteUseCase
import com.choo.moviefinder.domain.usecase.IsInWatchlistUseCase
import com.choo.moviefinder.domain.usecase.SaveMemoUseCase
import com.choo.moviefinder.domain.usecase.SaveWatchHistoryUseCase
import com.choo.moviefinder.domain.usecase.SetUserRatingUseCase
import com.choo.moviefinder.domain.usecase.ToggleFavoriteUseCase
import com.choo.moviefinder.domain.usecase.ToggleWatchlistUseCase
import com.choo.moviefinder.domain.usecase.UpdateMemoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fetch: DetailFetchUseCases,
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
    private val watchGoalNotificationHelper: WatchGoalNotificationHelper,
    private val getTmdbAccessTokenUseCase: GetTmdbAccessTokenUseCase,
    private val submitTmdbRatingUseCase: SubmitTmdbRatingUseCase
) : ViewModel() {

    private val movieId: Int = requireNotNull(savedStateHandle.get<Int>("movieId")) {
        "movieId argument is required for DetailViewModel"
    }

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _snackbarEvent = Channel<ErrorType>(Channel.CONFLATED)
    val snackbarEvent = _snackbarEvent.receiveAsFlow()

    private var loadingJob: Job? = null
    private val toggleMutex = Mutex()

    val isTmdbConnected: StateFlow<Boolean> = getTmdbAccessTokenUseCase()
        .map { it != null }
        .stateIn(viewModelScope, WhileSubscribed5s, false)

    private val _tmdbRatingResult = Channel<Boolean>(Channel.CONFLATED)
    val tmdbRatingResult = _tmdbRatingResult.receiveAsFlow()

    val isFavorite = isFavoriteUseCase(movieId)
        .stateIn(viewModelScope, WhileSubscribed5s, false)

    val isInWatchlist = isInWatchlistUseCase(movieId)
        .stateIn(viewModelScope, WhileSubscribed5s, false)

    private val memoDelegate = MemoDelegate(
        getMemosUseCase = getMemosUseCase,
        saveMemoUseCase = saveMemoUseCase,
        updateMemoUseCase = updateMemoUseCase,
        deleteMemoUseCase = deleteMemoUseCase,
        movieId = movieId,
        viewModelScope = viewModelScope,
        snackbarChannel = _snackbarEvent
    )

    private val userRatingDelegate = UserRatingDelegate(
        getUserRatingUseCase = getUserRatingUseCase,
        setUserRatingUseCase = setUserRatingUseCase,
        deleteUserRatingUseCase = deleteUserRatingUseCase,
        movieId = movieId,
        viewModelScope = viewModelScope,
        snackbarChannel = _snackbarEvent
    )

    val userRating get() = userRatingDelegate.userRating

    init {
        loadMovieDetail()
    }

    // 핵심 데이터(영화 상세 + 등급) 먼저 표시 후 나머지 API를 점진적으로 업데이트
    fun loadMovieDetail() {
        if (loadingJob?.isActive == true) return
        loadingJob = viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                // 1단계: 핵심 데이터 (영화 상세 + 등급) 병렬 로드
                val detail: MovieDetail
                val certification: String?
                coroutineScope {
                    val detailDeferred = async { fetch.getMovieDetail(movieId) }
                    val certDeferred = async {
                        loadOptionalNullable("cert") { fetch.getMovieCertification(movieId) }
                    }
                    detail = detailDeferred.await()
                    certification = certDeferred.await()
                }

                // 핵심 데이터로 즉시 Success emit (나머지는 null = 로딩 중)
                _uiState.value = DetailUiState.Success(
                    movieDetail = detail,
                    certification = certification
                )

                // 2단계: 보조 데이터 병렬 로드, 각각 완료 시 점진적 업데이트
                coroutineScope {
                    launch {
                        val credits = loadOptional("credits") { fetch.getMovieCredits(movieId) }
                        updateSuccess { it.copy(credits = credits) }
                    }
                    launch {
                        val similar = loadOptional("similar") { fetch.getSimilarMovies(movieId) }
                        updateSuccess { it.copy(similarMovies = similar) }
                    }
                    launch {
                        val reviews = loadOptional("reviews") { fetch.getMovieReviews(movieId) }
                        updateSuccess { it.copy(reviews = reviews) }
                    }
                    launch {
                        val trailer = loadOptionalNullable("trailer") { fetch.getMovieTrailer(movieId) }
                        updateSuccess { it.copy(trailerKey = trailer) }
                    }
                    launch {
                        val recs = loadOptional("recommendations") { fetch.getMovieRecommendations(movieId) }
                        updateSuccess { it.copy(recommendations = recs) }
                    }
                }

                saveWatchHistory(detail)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(ErrorMessageProvider.getErrorType(e))
            }
        }
    }

    // 현재 상태가 Success인 경우에만 필드를 업데이트
    private fun updateSuccess(transform: (DetailUiState.Success) -> DetailUiState.Success) {
        _uiState.update { current ->
            if (current is DetailUiState.Success) transform(current) else current
        }
    }

    // 부분 실패 허용 로드 (실패 시 빈 리스트 반환)
    private suspend fun <T> loadOptional(tag: String, block: suspend () -> List<T>): List<T> =
        suspendRunCatching { block() }
            .onFailure { Timber.w(it, "영화 %d의 %s 로드 실패", movieId, tag) }
            .getOrElse { emptyList() }

    // 부분 실패 허용 로드 (실패 시 null 반환)
    private suspend fun <T> loadOptionalNullable(tag: String, block: suspend () -> T?): T? =
        suspendRunCatching { block() }
            .onFailure { Timber.w(it, "영화 %d의 %s 로드 실패", movieId, tag) }
            .getOrNull()

    // 영화 상세 화면 진입 시 장르 정보와 함께 시청 기록을 Room DB에 저장하고 목표 달성을 확인한다
    private suspend fun saveWatchHistory(detail: MovieDetail) {
        val movie = detail.toMovie()
        val genres = detail.genres.joinToString(",") { it.name }
        suspendRunCatching { saveWatchHistoryUseCase(movie, genres) }
            .onFailure { Timber.w(it, "영화 %d 시청 기록 저장 실패", movieId) }
        suspendRunCatching { watchGoalNotificationHelper.checkAndNotifyGoalAchieved() }
            .onFailure { Timber.w(it, "영화 %d 시청 목표 확인 실패", movieId) }
    }

    // 즐겨찾기 상태 토글 (에러 시 Snackbar 이벤트 전송)
    fun toggleFavorite() = viewModelScope.launchWithErrorHandler(
        onError = { _snackbarEvent.trySend(it) }
    ) {
        toggleMutex.withLock {
            val state = _uiState.value
            if (state is DetailUiState.Success) {
                toggleFavoriteUseCase(state.movieDetail.toMovie())
            }
        }
    }

    // 워치리스트 토글 및 개봉일 알림 예약/취소
    fun toggleWatchlist() = viewModelScope.launchWithErrorHandler(
        onError = { _snackbarEvent.trySend(it) }
    ) {
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
    fun setUserRating(rating: Float) = userRatingDelegate.setUserRating(rating)

    // 사용자 영화 평점을 Room DB에서 삭제
    fun deleteUserRating() = userRatingDelegate.deleteUserRating()

    val memos get() = memoDelegate.memos

    // 영화에 새 메모를 저장
    fun saveMemo(content: String) = memoDelegate.saveMemo(content)

    // 기존 메모 내용을 수정
    fun updateMemo(memoId: Long, content: String) = memoDelegate.updateMemo(memoId, content)

    // 메모를 삭제
    fun deleteMemo(memoId: Long) = memoDelegate.deleteMemo(memoId)

    // TMDB에 영화 평점을 제출한다 (1.0~10.0, RatingBar 0.5~5.0 × 2)
    fun submitTmdbRating(rating: Float) {
        viewModelScope.launch {
            try {
                val success = submitTmdbRatingUseCase(movieId, rating)
                _tmdbRatingResult.trySend(success)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _tmdbRatingResult.trySend(false)
            }
        }
    }

}