package com.choo.moviefinder.presentation.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.choo.moviefinder.domain.model.Genre
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.usecase.ClearSearchHistoryUseCase
import com.choo.moviefinder.domain.usecase.DeleteSearchQueryUseCase
import com.choo.moviefinder.domain.usecase.DiscoverMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetGenreListUseCase
import com.choo.moviefinder.domain.usecase.GetRecentSearchesUseCase
import com.choo.moviefinder.domain.usecase.SaveSearchQueryUseCase
import com.choo.moviefinder.domain.usecase.SearchMoviesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val searchMoviesUseCase: SearchMoviesUseCase,
    private val discoverMoviesUseCase: DiscoverMoviesUseCase,
    private val getGenreListUseCase: GetGenreListUseCase,
    private val getRecentSearchesUseCase: GetRecentSearchesUseCase,
    private val saveSearchQueryUseCase: SaveSearchQueryUseCase,
    private val deleteSearchQueryUseCase: DeleteSearchQueryUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow(savedStateHandle.get<String>(KEY_QUERY) ?: "")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedYear = MutableStateFlow(savedStateHandle.get<Int>(KEY_YEAR))
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()

    private val _selectedGenres = MutableStateFlow(
        savedStateHandle.get<IntArray>(KEY_GENRES)?.toSet() ?: emptySet()
    )
    val selectedGenres: StateFlow<Set<Int>> = _selectedGenres.asStateFlow()

    private val _sortBy = MutableStateFlow(
        savedStateHandle.get<String>(KEY_SORT)?.let { name ->
            runCatching { SortOption.valueOf(name) }.getOrDefault(SortOption.POPULARITY_DESC)
        } ?: SortOption.POPULARITY_DESC
    )
    val sortBy: StateFlow<SortOption> = _sortBy.asStateFlow()

    private val _genres = MutableStateFlow<List<Genre>>(emptyList())
    val genres: StateFlow<List<Genre>> = _genres.asStateFlow()

    private var genreLoadFailed = false

    val recentSearches = getRecentSearchesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 키보드 검색/최근 검색어 클릭 시 debounce 없이 즉시 검색
    private val _immediateSearch = MutableSharedFlow<SearchParams>(extraBufferCapacity = 1)

    // 타이핑 중 자동 검색: 300ms debounce
    // 명시적 검색 액션: 즉시 실행 (merge)
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: Flow<PagingData<Movie>> = merge(
        combine(_searchQuery, _selectedYear, _selectedGenres, _sortBy) { query, year, genres, sort ->
            SearchParams(query, year, genres, sort)
        }.debounce(300),
        _immediateSearch
    )
        .filter { it.query.isNotBlank() || it.genres.isNotEmpty() }
        .distinctUntilChanged()
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
    }

    private fun loadGenres() {
        viewModelScope.launch {
            try {
                _genres.value = getGenreListUseCase()
                genreLoadFailed = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                genreLoadFailed = true
                Timber.w(e, "Failed to load genre list")
            }
        }
    }

    fun retryLoadGenres() {
        if (genreLoadFailed) loadGenres()
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
    }
}

enum class SortOption(val apiValue: String) {
    POPULARITY_DESC("popularity.desc"),
    VOTE_AVERAGE_DESC("vote_average.desc"),
    RELEASE_DATE_DESC("release_date.desc"),
    REVENUE_DESC("revenue.desc")
}
