package com.choo.moviefinder.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.choo.moviefinder.domain.usecase.GetNowPlayingMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetPopularMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetTrendingMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetUpcomingMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetWatchHistoryUseCase
import com.choo.moviefinder.core.util.WhileSubscribed5s
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class HomeTab { NOW_PLAYING, POPULAR, TRENDING, UPCOMING }

@HiltViewModel
class HomeViewModel @Inject constructor(
    getNowPlayingMoviesUseCase: GetNowPlayingMoviesUseCase,
    getPopularMoviesUseCase: GetPopularMoviesUseCase,
    getTrendingMoviesUseCase: GetTrendingMoviesUseCase,
    getUpcomingMoviesUseCase: GetUpcomingMoviesUseCase,
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

    val upcomingMovies by lazy(LazyThreadSafetyMode.NONE) {
        getUpcomingMoviesUseCase().cachedIn(viewModelScope)
    }

    val watchHistory = getWatchHistoryUseCase()
        .stateIn(viewModelScope, WhileSubscribed5s, emptyList())

    fun onTabSelected(tab: HomeTab) {
        _selectedTab.value = tab
    }
}
