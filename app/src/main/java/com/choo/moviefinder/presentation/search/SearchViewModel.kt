package com.choo.moviefinder.presentation.search

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMoviesUseCase: SearchMoviesUseCase,
    private val getRecentSearchesUseCase: GetRecentSearchesUseCase,
    private val saveSearchQueryUseCase: SaveSearchQueryUseCase,
    private val deleteSearchQueryUseCase: DeleteSearchQueryUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedYear = MutableStateFlow<Int?>(null)
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()

    val recentSearches = getRecentSearchesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: Flow<PagingData<Movie>> = combine(_searchQuery, _selectedYear) { query, year ->
        Pair(query, year)
    }
        .debounce(300)
        .filter { it.first.isNotBlank() }
        .distinctUntilChanged()
        .flatMapLatest { (query, year) ->
            searchMoviesUseCase(query, year)
        }
        .cachedIn(viewModelScope)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onYearSelected(year: Int?) {
        _selectedYear.value = year
    }

    fun onSearch(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            saveSearchQueryUseCase(query)
        }
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
}