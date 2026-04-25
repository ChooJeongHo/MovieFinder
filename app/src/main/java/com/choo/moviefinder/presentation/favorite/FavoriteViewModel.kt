package com.choo.moviefinder.presentation.favorite

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import com.choo.moviefinder.core.notification.WatchlistReminderWorker
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.core.util.WhileSubscribed5s
import com.choo.moviefinder.core.util.launchWithErrorHandler
import com.choo.moviefinder.core.util.PosterTagSuggester
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.WatchlistReminder
import com.choo.moviefinder.domain.model.MovieTag
import com.choo.moviefinder.domain.usecase.AddTagToMovieUseCase
import com.choo.moviefinder.domain.usecase.GetAllTagNamesUseCase
import com.choo.moviefinder.domain.usecase.GetFavoriteMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetFavoritesByTagUseCase
import com.choo.moviefinder.domain.usecase.GetTagsForMovieUseCase
import com.choo.moviefinder.domain.usecase.ClearWatchlistReminderUseCase
import com.choo.moviefinder.domain.usecase.GetWatchlistRemindersUseCase
import com.choo.moviefinder.domain.usecase.GetWatchlistUseCase
import com.choo.moviefinder.domain.usecase.RemoveTagFromMovieUseCase
import com.choo.moviefinder.domain.usecase.SetWatchlistReminderUseCase
import com.choo.moviefinder.domain.usecase.ToggleFavoriteUseCase
import com.choo.moviefinder.domain.usecase.ToggleWatchlistUseCase
import com.choo.moviefinder.domain.repository.UserRatingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import java.util.concurrent.TimeUnit
import androidx.work.WorkManager
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val getFavoriteMoviesUseCase: GetFavoriteMoviesUseCase,
    private val getFavoritesByTagUseCase: GetFavoritesByTagUseCase,
    private val getWatchlistUseCase: GetWatchlistUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val toggleWatchlistUseCase: ToggleWatchlistUseCase,
    private val getTagsForMovieUseCase: GetTagsForMovieUseCase,
    getAllTagNamesUseCase: GetAllTagNamesUseCase,
    private val addTagToMovieUseCase: AddTagToMovieUseCase,
    private val removeTagFromMovieUseCase: RemoveTagFromMovieUseCase,
    private val posterTagSuggester: PosterTagSuggester,
    private val userRatingRepository: UserRatingRepository,
    private val setWatchlistReminderUseCase: SetWatchlistReminderUseCase,
    private val clearWatchlistReminderUseCase: ClearWatchlistReminderUseCase,
    private val getWatchlistRemindersUseCase: GetWatchlistRemindersUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val workManager by lazy { WorkManager.getInstance(context) }

    private val _sortOrder = MutableStateFlow(FavoriteSortOrder.ADDED_DATE)
    val sortOrder: StateFlow<FavoriteSortOrder> = _sortOrder.asStateFlow()

    // 다중 태그 필터: 선택된 태그 집합 (empty = 전체 표시)
    private val _tagFilters = MutableStateFlow<Set<String>>(emptySet())
    val tagFilters: StateFlow<Set<String>> = _tagFilters.asStateFlow()

    // 평점 필터: 최소 평점 (0f = 필터 없음)
    private val _minRating = MutableStateFlow(0f)
    val minRating: StateFlow<Float> = _minRating.asStateFlow()

    // 모든 고유 태그 이름 목록 (필터 칩 표시용)
    val allTagNames: StateFlow<List<String>> = getAllTagNamesUseCase()
        .stateIn(viewModelScope, WhileSubscribed5s, emptyList())

    // 모든 사용자 평점 맵 (movieId → rating)
    private val userRatings: StateFlow<Map<Int, Float>> = userRatingRepository.getAllUserRatings()
        .stateIn(viewModelScope, WhileSubscribed5s, emptyMap())

    // 선택된 태그·정렬 순서에 따라 DB ORDER BY로 정렬된 즐겨찾기 기본 목록
    private val baseFavoriteMovies: StateFlow<List<Movie>> =
        combine(_sortOrder, _tagFilters) { sort, tags -> sort to tags }
            .distinctUntilChanged()
            .flatMapLatest { (sort, tags) ->
                when {
                    tags.isEmpty() -> getFavoriteMoviesUseCase(sort)
                    tags.size == 1 -> getFavoritesByTagUseCase(tags.first(), sort)
                    else -> {
                        // 다중 태그: 각 태그 Flow를 combine하여 OR 합집합 처리
                        val flows = tags.map { tag -> getFavoritesByTagUseCase(tag, sort) }
                        combine(flows) { arrays ->
                            val seen = mutableSetOf<Int>()
                            val result = mutableListOf<Movie>()
                            arrays.forEach { list ->
                                list.forEach { movie ->
                                    if (seen.add(movie.id)) result.add(movie)
                                }
                            }
                            result
                        }
                    }
                }
            }
            .stateIn(viewModelScope, WhileSubscribed5s, emptyList())

    // 평점 필터를 적용한 최종 즐겨찾기 목록
    val favoriteMovies: StateFlow<List<Movie>> =
        combine(baseFavoriteMovies, userRatings, _minRating) { movies, ratings, minRating ->
            if (minRating <= 0f) {
                movies
            } else {
                movies.filter { movie ->
                    val userRating = ratings[movie.id] ?: 0f
                    userRating >= minRating
                }
            }
        }.stateIn(viewModelScope, WhileSubscribed5s, emptyList())

    // 정렬 순서에 따라 DB ORDER BY로 정렬된 워치리스트 목록
    val watchlistMovies: StateFlow<List<Movie>> = _sortOrder.flatMapLatest { sort ->
        getWatchlistUseCase(sort)
    }.stateIn(viewModelScope, WhileSubscribed5s, emptyList())

    private val _snackbarEvent = Channel<ErrorType>(Channel.CONFLATED)
    val snackbarEvent = _snackbarEvent.receiveAsFlow()

    private val _reminderSnackbar = Channel<String>(Channel.CONFLATED)
    val reminderSnackbar = _reminderSnackbar.receiveAsFlow()

    val scheduledReminders: StateFlow<List<WatchlistReminder>> = getWatchlistRemindersUseCase.asFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 즐겨찾기 상태 토글 (에러 시 Snackbar 이벤트 전송)
    fun toggleFavorite(movie: Movie) = viewModelScope.launchWithErrorHandler(
        onError = {
            Timber.e("영화 %d 즐겨찾기 토글 실패", movie.id)
            _snackbarEvent.trySend(it)
        }
    ) {
        toggleFavoriteUseCase(movie)
    }

    // 정렬 순서 변경
    fun onSortOrderSelected(sort: FavoriteSortOrder) {
        _sortOrder.value = sort
    }

    // 태그 필터 토글: 이미 선택된 태그면 제거, 아니면 추가 (null = 전체 초기화)
    fun toggleTagFilter(tag: String?) {
        if (tag == null) {
            _tagFilters.value = emptySet()
        } else {
            val current = _tagFilters.value
            _tagFilters.value = if (tag in current) current - tag else current + tag
        }
    }

    // 하위 호환: 단일 태그 선택 (워치리스트 탭 전환 시 초기화에 사용)
    fun onTagSelected(tag: String?) {
        _tagFilters.value = if (tag == null) emptySet() else setOf(tag)
    }

    // selectedTag: 단일 태그 선택 상태 노출 (하위 호환용 — null = 전체)
    val selectedTag: StateFlow<String?> = _tagFilters
        .map { tags -> tags.singleOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // 평점 필터 설정 (0f = 필터 없음)
    fun setMinRating(rating: Float) {
        _minRating.value = rating
    }

    // 워치리스트 상태 토글 (에러 시 Snackbar 이벤트 전송)
    fun toggleWatchlist(movie: Movie) = viewModelScope.launchWithErrorHandler(
        onError = {
            Timber.e("영화 %d 워치리스트 토글 실패", movie.id)
            _snackbarEvent.trySend(it)
        }
    ) {
        toggleWatchlistUseCase(movie)
    }

    // 특정 영화의 태그 목록 Flow 반환
    fun getTagsForMovie(movieId: Int): Flow<List<MovieTag>> =
        getTagsForMovieUseCase(movieId)

    // 영화에 태그 추가 (에러 시 Snackbar 이벤트 전송)
    fun addTagToMovie(movieId: Int, tagName: String) = viewModelScope.launchWithErrorHandler(
        onError = {
            Timber.e("영화 %d에 태그 '%s' 추가 실패", movieId, tagName)
            _snackbarEvent.trySend(it)
        }
    ) {
        addTagToMovieUseCase(movieId, tagName)
    }

    // 포스터 이미지를 ML Kit으로 분석하여 태그 추천 목록 반환
    suspend fun suggestTagsForPoster(posterPath: String?): List<String> =
        posterTagSuggester.suggestTags(posterPath)

    // 영화에서 태그 제거 (에러 시 Snackbar 이벤트 전송)
    fun removeTagFromMovie(movieId: Int, tagName: String) = viewModelScope.launchWithErrorHandler(
        onError = {
            Timber.e("영화 %d에서 태그 '%s' 제거 실패", movieId, tagName)
            _snackbarEvent.trySend(it)
        }
    ) {
        removeTagFromMovieUseCase(movieId, tagName)
    }

    // 워치리스트 영화에 알림 날짜를 설정하고 WorkManager 작업을 예약한다
    fun setWatchlistReminder(movie: Movie, dateMillis: Long) =
        viewModelScope.launchWithErrorHandler(
            onError = {
                Timber.e("영화 %d 알림 설정 실패", movie.id)
                _snackbarEvent.trySend(it)
            }
        ) {
            setWatchlistReminderUseCase(movie.id, dateMillis)
            val delay = dateMillis - System.currentTimeMillis()
            if (delay <= 0) return@launchWithErrorHandler
            val inputData = workDataOf(
                WatchlistReminderWorker.KEY_MOVIE_ID to movie.id,
                WatchlistReminderWorker.KEY_MOVIE_TITLE to movie.title
            )
            val request = OneTimeWorkRequestBuilder<WatchlistReminderWorker>()
                .setInputData(inputData)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("watchlist_reminder")
                .build()
            workManager.enqueueUniqueWork(
                "watchlist_reminder_${movie.id}",
                ExistingWorkPolicy.REPLACE,
                request
            )
            _reminderSnackbar.trySend(context.getString(com.choo.moviefinder.R.string.reminder_set_confirmation))
        }

    // 워치리스트 영화의 알림을 취소한다
    fun clearWatchlistReminder(movieId: Int) = viewModelScope.launchWithErrorHandler(
        onError = {
            Timber.e("영화 %d 알림 취소 실패", movieId)
            _snackbarEvent.trySend(it)
        }
    ) {
        clearWatchlistReminderUseCase(movieId)
        workManager.cancelUniqueWork("watchlist_reminder_$movieId")
    }
}
