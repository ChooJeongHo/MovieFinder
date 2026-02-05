package com.choo.moviefinder.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.choo.moviefinder.domain.usecase.GetNowPlayingMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetPopularMoviesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    getNowPlayingMoviesUseCase: GetNowPlayingMoviesUseCase,
    getPopularMoviesUseCase: GetPopularMoviesUseCase
) : ViewModel() {

    val nowPlayingMovies = getNowPlayingMoviesUseCase()
        .cachedIn(viewModelScope)

    val popularMovies = getPopularMoviesUseCase()
        .cachedIn(viewModelScope)
}