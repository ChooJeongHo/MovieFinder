package com.choo.moviefinder.presentation.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.core.util.WhileSubscribed5s
import com.choo.moviefinder.domain.model.Genre
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.PersonSearchItem
import com.choo.moviefinder.domain.usecase.ClearSearchHistoryUseCase
import com.choo.moviefinder.domain.usecase.DeleteSearchQueryUseCase
import com.choo.moviefinder.domain.usecase.DiscoverMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetGenreListUseCase
import com.choo.moviefinder.domain.usecase.GetRecentSearchesUseCase
import com.choo.moviefinder.domain.usecase.GetWatchHistoryUseCase
import com.choo.moviefinder.domain.usecase.SaveSearchQueryUseCase
import com.choo.moviefinder.domain.usecase.SearchLocalMoviesUseCase
import com.choo.moviefinder.domain.usecase.SearchMoviesUseCase
import com.choo.moviefinder.domain.usecase.SearchPersonUseCase
import com.choo.moviefinder.presentation.adapter.MoviePagingAdapter.ViewMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions")
class SearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val searchMoviesUseCase: SearchMoviesUseCase,
    private val discoverMoviesUseCase: DiscoverMoviesUseCase,
    private val getGenreListUseCase: GetGenreListUseCase,
    private val getRecentSearchesUseCase: GetRecentSearchesUseCase,
    private val saveSearchQueryUseCase: SaveSearchQueryUseCase,
    private val deleteSearchQueryUseCase: DeleteSearchQueryUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase,
    private val searchPersonUseCase: SearchPersonUseCase,
    private val searchLocalMoviesUseCase: SearchLocalMoviesUseCase,
    getWatchHistoryUseCase: GetWatchHistoryUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow(savedStateHandle.get<String>(KEY_QUERY) ?: "")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedYear = MutableStateFlow(savedStateHandle.get<Int>(KEY_YEAR))
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()

    private val _selectedGenres = MutableStateFlow(
        savedStateHandle.get<IntArray>(KEY_GENRES)?.toSet() ?: emptySet()
    )
    val selectedGenres: StateFlow<Set<Int>> = _selectedGenres.asStateFlow()

    private val _sortBy = MutableStateFlow(savedStateHandle.getEnum(KEY_SORT, SortOption.POPULARITY_DESC))
    val sortBy: StateFlow<SortOption> = _sortBy.asStateFlow()

    private val _viewMode = MutableStateFlow(savedStateHandle.getEnum(KEY_VIEW_MODE, ViewMode.GRID))
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _genres = MutableStateFlow<List<Genre>>(emptyList())
    val genres: StateFlow<List<Genre>> = _genres.asStateFlow()

    private val _genreLoadFailed = MutableStateFlow(false)

    private val _searchMode = MutableStateFlow(SearchMode.MOVIE)
    val searchMode: StateFlow<SearchMode> = _searchMode.asStateFlow()

    private val _personResults = MutableStateFlow<List<PersonSearchItem>>(emptyList())
    val personResults: StateFlow<List<PersonSearchItem>> = _personResults.asStateFlow()

    private val _personSearchQuery = MutableStateFlow("")

    private val _isPersonSearchLoading = MutableStateFlow(false)
    val isPersonSearchLoading: StateFlow<Boolean> = _isPersonSearchLoading.asStateFlow()

    private val _snackbarEvent = Channel<ErrorType>(Channel.CONFLATED)
    val snackbarEvent = _snackbarEvent.receiveAsFlow()

    private val _offlineResults = MutableStateFlow<List<Movie>>(emptyList())
    val offlineResults: StateFlow<List<Movie>> = _offlineResults.asStateFlow()

    val recentSearches = getRecentSearchesUseCase()
        .stateIn(viewModelScope, WhileSubscribed5s, emptyList())

    val watchHistory = getWatchHistoryUseCase()
        .stateIn(viewModelScope, WhileSubscribed5s, emptyList())

    // 키보드 검색/최근 검색어 클릭 시 debounce 없이 즉시 검색
    private val _immediateSearch = MutableSharedFlow<SearchParams>(extraBufferCapacity = 1)

    // 타이핑 중 자동 검색: 300ms debounce
    // 명시적 검색 액션: 즉시 실행 (merge)
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: Flow<PagingData<Movie>> = merge(
        combine(_searchQuery, _selectedYear, _selectedGenres, _sortBy) { query, year, genres, sort ->
            SearchParams(query, year, genres, sort)
        }.debounce(MOVIE_SEARCH_DEBOUNCE_MS).distinctUntilChanged(),
        _immediateSearch
    )
        .filter { it.query.isNotBlank() || it.genres.isNotEmpty() }
        .flatMapLatest { params ->
            if (params.query.isNotBlank()) {
                searchMoviesUseCase(params.query, params.year)
            } else {
                discoverMoviesUseCase(params.genres, params.sort.apiValue, params.year)
            }
        }
        .cachedIn(viewModelScope)

    init {
        loadGenres()
        collectPersonSearchQuery()
    }

    // 배우 검색 쿼리를 300ms debounce 후 API 호출 (flatMapLatest로 새 쿼리가 인플라이트 호출을 취소)
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun collectPersonSearchQuery() {
        viewModelScope.launch {
            _personSearchQuery
                .debounce(PERSON_SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    flow {
                        val trimmed = query.trim()
                        if (trimmed.isBlank()) {
                            emit(Result.success<List<PersonSearchItem>>(emptyList()))
                            return@flow
                        }
                        emit(Result.success(null)) // loading sentinel
                        try {
                            emit(Result.success(searchPersonUseCase(trimmed)))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Timber.w(e, "배우 검색 실패, 검색어: $trimmed")
                            emit(Result.failure(e))
                        }
                    }
                }
                .collect { result ->
                    when {
                        result.isFailure -> {
                            _personResults.value = emptyList()
                            _isPersonSearchLoading.value = false
                            _snackbarEvent.trySend(ErrorMessageProvider.getErrorType(result.exceptionOrNull()!!))
                        }
                        result.getOrNull() == null -> {
                            // loading sentinel
                            _isPersonSearchLoading.value = true
                        }
                        else -> {
                            _personResults.value = result.getOrNull()!!
                            _isPersonSearchLoading.value = false
                        }
                    }
                }
        }
    }

    // TMDB API에서 장르 목록을 로드하여 StateFlow 갱신
    private fun loadGenres() {
        viewModelScope.launch {
            try {
                _genres.value = getGenreListUseCase()
                _genreLoadFailed.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _genreLoadFailed.value = true
                Timber.w(e, "장르 목록 로드 실패")
            }
        }
    }

    // 장르 로드 실패 시 재시도
    fun retryLoadGenres() {
        if (_genreLoadFailed.value) loadGenres()
    }

    // 검색어 변경 시 StateFlow 갱신 및 SavedStateHandle 저장
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        savedStateHandle[KEY_QUERY] = query
    }

    // 연도 필터 선택 시 StateFlow 갱신 및 SavedStateHandle 저장
    fun onYearSelected(year: Int?) {
        _selectedYear.value = year
        savedStateHandle[KEY_YEAR] = year
    }

    // 장르 필터 선택 시 StateFlow 갱신 및 SavedStateHandle 저장
    fun onGenresSelected(genreIds: Set<Int>) {
        _selectedGenres.value = genreIds
        savedStateHandle[KEY_GENRES] = genreIds.toIntArray()
    }

    // 정렬 옵션 선택 시 StateFlow 갱신 및 SavedStateHandle 저장
    fun onSortSelected(sort: SortOption) {
        _sortBy.value = sort
        savedStateHandle[KEY_SORT] = sort.name
    }

    // 검색어 trim 후 DB에 저장하고 즉시 검색 실행
    fun onSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            saveSearchQueryUseCase(trimmed)
        }
        _immediateSearch.tryEmit(
            SearchParams(trimmed, _selectedYear.value, _selectedGenres.value, _sortBy.value)
        )
    }

    // 그리드/리스트 보기 모드 전환 및 SavedStateHandle 저장
    fun toggleViewMode() {
        val newMode = if (_viewMode.value == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
        _viewMode.value = newMode
        savedStateHandle[KEY_VIEW_MODE] = newMode.name
    }

    // 검색어 없이 장르/정렬 필터만으로 Discover API 즉시 호출
    fun onDiscoverWithFilters() {
        if (_selectedGenres.value.isEmpty()) return
        _immediateSearch.tryEmit(
            SearchParams("", _selectedYear.value, _selectedGenres.value, _sortBy.value)
        )
    }

    // 특정 최근 검색어를 DB에서 삭제
    fun onDeleteRecentSearch(query: String) {
        viewModelScope.launch {
            deleteSearchQueryUseCase(query)
        }
    }

    // 전체 검색 기록을 DB에서 삭제
    fun onClearSearchHistory() {
        viewModelScope.launch {
            clearSearchHistoryUseCase()
        }
    }

    // 영화/배우 검색 모드 전환
    fun toggleSearchMode() {
        _searchMode.value = if (_searchMode.value == SearchMode.MOVIE) SearchMode.PERSON else SearchMode.MOVIE
        _personSearchQuery.value = ""
        _personResults.value = emptyList()
    }

    // 배우 검색 쿼리를 StateFlow에 전달 (300ms debounce 적용)
    fun onPersonSearch(query: String) {
        _personSearchQuery.value = query
    }

    // 오프라인 상태에서 즐겨찾기 + 워치리스트에서 로컬 검색 실행
    fun searchOffline(query: String) {
        if (query.isBlank()) {
            _offlineResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                _offlineResults.value = searchLocalMoviesUseCase(query)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "로컬 오프라인 검색 실패, 검색어: $query")
                _offlineResults.value = emptyList()
            }
        }
    }

    // 오프라인 검색 결과 초기화
    fun clearOfflineResults() {
        _offlineResults.value = emptyList()
    }

    private data class SearchParams(
        val query: String,
        val year: Int?,
        val genres: Set<Int>,
        val sort: SortOption
    )

    companion object {
        private const val KEY_QUERY = "search_query"
        private const val KEY_YEAR = "selected_year"
        private const val KEY_GENRES = "selected_genres"
        private const val KEY_SORT = "selected_sort"
        private const val KEY_VIEW_MODE = "view_mode"
        private const val MOVIE_SEARCH_DEBOUNCE_MS = 300L
        private const val PERSON_SEARCH_DEBOUNCE_MS = 300L
    }
}

private inline fun <reified T : Enum<T>> SavedStateHandle.getEnum(key: String, default: T): T =
    get<String>(key)?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

enum class SearchMode {
    MOVIE,
    PERSON,
}

enum class SortOption(val apiValue: String) {
    POPULARITY_DESC("popularity.desc"),
    VOTE_AVERAGE_DESC("vote_average.desc"),
    RELEASE_DATE_DESC("release_date.desc"),
    REVENUE_DESC("revenue.desc")
}
