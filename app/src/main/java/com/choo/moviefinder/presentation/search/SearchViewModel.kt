package com.choo.moviefinder.presentation.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.usecase.ClearSearchHistoryUseCase
import com.choo.moviefinder.domain.usecase.DeleteSearchQueryUseCase
import com.choo.moviefinder.domain.usecase.GetRecentSearchesUseCase
import com.choo.moviefinder.domain.usecase.SaveSearchQueryUseCase
import com.choo.moviefinder.domain.usecase.SearchMoviesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val searchMoviesUseCase: SearchMoviesUseCase,
    private val getRecentSearchesUseCase: GetRecentSearchesUseCase,
    private val saveSearchQueryUseCase: SaveSearchQueryUseCase,
    private val deleteSearchQueryUseCase: DeleteSearchQueryUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow(savedStateHandle.get<String>(KEY_QUERY) ?: "")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedYear = MutableStateFlow(savedStateHandle.get<Int>(KEY_YEAR))
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()

    val recentSearches = getRecentSearchesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 키보드 검색/최근 검색어 클릭 시 debounce 없이 즉시 검색
    private val _immediateSearch = MutableSharedFlow<Pair<String, Int?>>(extraBufferCapacity = 1)

    // 타이핑 중 자동 검색: 300ms debounce
    // 명시적 검색 액션: 즉시 실행 (merge)
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: Flow<PagingData<Movie>> = merge(
        combine(_searchQuery, _selectedYear) { query, year -> Pair(query, year) }
            .debounce(300),
        _immediateSearch
    )
        .filter { it.first.isNotBlank() }
        .distinctUntilChanged()
        .flatMapLatest { (query, year) ->
            searchMoviesUseCase(query, year)
        }
        .cachedIn(viewModelScope)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        savedStateHandle[KEY_QUERY] = query
    }

    fun onYearSelected(year: Int?) {
        _selectedYear.value = year
        savedStateHandle[KEY_YEAR] = year
    }

    fun onSearch(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            saveSearchQueryUseCase(query)
        }
        _immediateSearch.tryEmit(Pair(query, _selectedYear.value))
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

    companion object {
        private const val KEY_QUERY = "search_query"
        private const val KEY_YEAR = "selected_year"
    }
}