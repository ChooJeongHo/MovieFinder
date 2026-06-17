package com.choo.moviefinder.presentation.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.core.util.WhileSubscribed5s
import com.choo.moviefinder.core.util.getEnum
import com.choo.moviefinder.core.util.suspendRunCatching
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
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
        .map { it.take(WATCH_HISTORY_SUGGESTION_LIMIT) }
        .stateIn(viewModelScope, WhileSubscribed5s, emptyList())

    // 키보드 검색/최근 검색어 클릭 시 debounce 없이 즉시 검색
    private val _immediateSearch = MutableSharedFlow<SearchParams>(extraBufferCapacity = 1)

    // 타이핑 중 자동 검색: 300ms debounce
    // 명시적 검색 액션: 즉시 실행 (merge)
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: Flow<PagingData<Movie>> = merge(
        combine(_searchQuery, _selectedYear, _selectedGenres, _sortBy) { query, year, genres, sort ->
            SearchParams(query, year, genres, sort)
        }.debounce(SEARCH_DEBOUNCE_MS).distinctUntilChanged(),
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
                .debounce(SEARCH_DEBOUNCE_MS)
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
                    // 모드가 변경된 경우 배우 검색 결과를 무시한다 (PERSON → MOVIE 전환 후 인플라이트 응답 블리드 방지)
                    if (_searchMode.value != SearchMode.PERSON) return@collect
                    when {
                        result.isFailure -> {
                            _personResults.value = emptyList()
                            _isPersonSearchLoading.value = false
                            val error = result.exceptionOrNull() ?: Exception("Unknown error")
                            _snackbarEvent.trySend(ErrorMessageProvider.getErrorType(error))
                        }
                        result.getOrNull() == null -> {
                            _isPersonSearchLoading.value = true
                        }
                        else -> {
                            _personResults.value = result.getOrNull() ?: return@collect
                            _isPersonSearchLoading.value = false
                        }
                    }
                }
        }
    }

    // TMDB API에서 장르 목록을 로드하여 StateFlow 갱신
    private fun loadGenres() {
        viewModelScope.launch {
            suspendRunCatching {
                _genres.value = getGenreListUseCase()
                _genreLoadFailed.value = false
            }.onFailure { e ->
                _genreLoadFailed.value = true
                Timber.w(e, "장르 목록 로드 실패")
            }
        }
    }

    fun retryLoadGenres() {
        if (_genreLoadFailed.value) loadGenres()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        savedStateHandle[KEY_QUERY] = query
    }

    fun onYearSelected(year: Int?) {
        _selectedYear.value = year
        savedStateHandle[KEY_YEAR] = year
    }

    fun onGenresSelected(genreIds: Set<Int>) {
        _selectedGenres.value = genreIds
        savedStateHandle[KEY_GENRES] = genreIds.toIntArray()
    }

    fun onSortSelected(sort: SortOption) {
        _sortBy.value = sort
        savedStateHandle[KEY_SORT] = sort.name
    }

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

    fun toggleViewMode() {
        val newMode = if (_viewMode.value == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
        _viewMode.value = newMode
        savedStateHandle[KEY_VIEW_MODE] = newMode.name
    }

    fun onDiscoverWithFilters() {
        if (_selectedGenres.value.isEmpty()) return
        _immediateSearch.tryEmit(
            SearchParams("", _selectedYear.value, _selectedGenres.value, _sortBy.value)
        )
    }

    fun onDeleteRecentSearch(query: String) {
        viewModelScope.launch {
            deleteSearchQueryUseCase(query)
        }
    }

    fun onClearSearchHistory() {
        viewModelScope.launch {
            clearSearchHistoryUseCase()
        }
    }

    fun toggleSearchMode() {
        _searchMode.value = when (_searchMode.value) {
            SearchMode.MOVIE -> SearchMode.PERSON
            SearchMode.PERSON -> SearchMode.MOVIE
        }
        _personSearchQuery.value = ""
        _personResults.value = emptyList()
        _isPersonSearchLoading.value = false
    }

    fun onPersonSearch(query: String) {
        _personSearchQuery.value = query
    }

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
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val WATCH_HISTORY_SUGGESTION_LIMIT = 3
    }
}
