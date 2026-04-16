package com.choo.moviefinder.presentation.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.core.util.launchWithErrorHandler
import com.choo.moviefinder.core.util.PosterTagSuggester
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.MovieTag
import com.choo.moviefinder.domain.usecase.AddTagToMovieUseCase
import com.choo.moviefinder.domain.usecase.GetAllTagNamesUseCase
import com.choo.moviefinder.domain.usecase.GetFavoriteMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetFavoritesByTagUseCase
import com.choo.moviefinder.domain.usecase.GetTagsForMovieUseCase
import com.choo.moviefinder.domain.usecase.GetWatchlistUseCase
import com.choo.moviefinder.domain.usecase.RemoveTagFromMovieUseCase
import com.choo.moviefinder.domain.usecase.ToggleFavoriteUseCase
import com.choo.moviefinder.domain.usecase.ToggleWatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
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
    private val posterTagSuggester: PosterTagSuggester
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(FavoriteSortOrder.ADDED_DATE)
    val sortOrder: StateFlow<FavoriteSortOrder> = _sortOrder.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    // 모든 고유 태그 이름 목록 (필터 칩 표시용)
    val allTagNames: StateFlow<List<String>> = getAllTagNamesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 선택된 태그·정렬 순서에 따라 DB ORDER BY로 정렬된 즐겨찾기 목록
    val favoriteMovies: StateFlow<List<Movie>> = combine(_sortOrder, _selectedTag) { sort, tag -> sort to tag }
        .distinctUntilChanged()
        .flatMapLatest { (sort, tag) ->
            if (tag == null) {
                getFavoriteMoviesUseCase(sort)
            } else {
                getFavoritesByTagUseCase(tag, sort)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 정렬 순서에 따라 DB ORDER BY로 정렬된 워치리스트 목록
    val watchlistMovies: StateFlow<List<Movie>> = _sortOrder.flatMapLatest { sort ->
        getWatchlistUseCase(sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _snackbarEvent = Channel<ErrorType>(Channel.CONFLATED)
    val snackbarEvent = _snackbarEvent.receiveAsFlow()

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

    // 태그 필터 선택 (null = 전체 표시)
    fun onTagSelected(tag: String?) {
        _selectedTag.value = tag
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
}
