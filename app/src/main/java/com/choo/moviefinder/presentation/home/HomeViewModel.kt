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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

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

    private val nowPlayingMovies by lazy(LazyThreadSafetyMode.NONE) {
        getNowPlayingMoviesUseCase().cachedIn(viewModelScope)
    }

    private val popularMovies by lazy(LazyThreadSafetyMode.NONE) {
        getPopularMoviesUseCase().cachedIn(viewModelScope)
    }

    private val trendingMovies by lazy(LazyThreadSafetyMode.NONE) {
        getTrendingMoviesUseCase().cachedIn(viewModelScope)
    }

    private val upcomingMovies by lazy(LazyThreadSafetyMode.NONE) {
        getUpcomingMoviesUseCase().cachedIn(viewModelScope)
    }

    val currentMovies = _selectedTab.flatMapLatest { tab ->
        when (tab) {
            HomeTab.NOW_PLAYING -> nowPlayingMovies
            HomeTab.POPULAR -> popularMovies
            HomeTab.TRENDING -> trendingMovies
            HomeTab.UPCOMING -> upcomingMovies
        }
    }

    val watchHistory = getWatchHistoryUseCase()
        .map { it.take(20) }
        .stateIn(viewModelScope, WhileSubscribed5s, emptyList())

    fun onTabSelected(tab: HomeTab) {
        _selectedTab.value = tab
    }
}
