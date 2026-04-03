package com.choo.moviefinder.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.choo.moviefinder.domain.usecase.GetNowPlayingMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetPopularMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetTrendingMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetWatchHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class HomeTab { NOW_PLAYING, POPULAR, TRENDING }

@HiltViewModel
class HomeViewModel @Inject constructor(
    getNowPlayingMoviesUseCase: GetNowPlayingMoviesUseCase,
    getPopularMoviesUseCase: GetPopularMoviesUseCase,
    getTrendingMoviesUseCase: GetTrendingMoviesUseCase,
    getWatchHistoryUseCase: GetWatchHistoryUseCase
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(HomeTab.NOW_PLAYING)
    val selectedTab: StateFlow<HomeTab> = _selectedTab.asStateFlow()

    val nowPlayingMovies by lazy {
        getNowPlayingMoviesUseCase().cachedIn(viewModelScope)
    }

    val popularMovies by lazy {
        getPopularMoviesUseCase().cachedIn(viewModelScope)
    }

    val trendingMovies by lazy {
        getTrendingMoviesUseCase().cachedIn(viewModelScope)
    }

    val watchHistory = getWatchHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onTabSelected(tab: HomeTab) {
        _selectedTab.value = tab
    }
}
