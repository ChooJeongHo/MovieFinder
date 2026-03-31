package com.choo.moviefinder.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.choo.moviefinder.domain.usecase.GetNowPlayingMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetPopularMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetTrendingMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetWatchHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    getNowPlayingMoviesUseCase: GetNowPlayingMoviesUseCase,
    getPopularMoviesUseCase: GetPopularMoviesUseCase,
    getTrendingMoviesUseCase: GetTrendingMoviesUseCase,
    getWatchHistoryUseCase: GetWatchHistoryUseCase
) : ViewModel() {

    val nowPlayingMovies = getNowPlayingMoviesUseCase()
        .cachedIn(viewModelScope)

    val popularMovies = getPopularMoviesUseCase()
        .cachedIn(viewModelScope)

    val trendingMovies = getTrendingMoviesUseCase()
        .cachedIn(viewModelScope)

    val watchHistory = getWatchHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
