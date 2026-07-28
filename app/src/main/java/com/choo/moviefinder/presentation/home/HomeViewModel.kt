package com.choo.moviefinder.presentation.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.choo.moviefinder.core.util.launchWithErrorHandler
import com.choo.moviefinder.domain.usecase.GetDailyBoxOfficeWithTmdbMatchUseCase
import com.choo.moviefinder.domain.usecase.GetNowPlayingMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetPopularMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetTrendingMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetUpcomingMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetWatchHistoryUseCase
import com.choo.moviefinder.domain.usecase.GetWeeklyBoxOfficeWithTmdbMatchUseCase
import com.choo.moviefinder.core.util.WhileSubscribed5s
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    getNowPlayingMoviesUseCase: GetNowPlayingMoviesUseCase,
    getPopularMoviesUseCase: GetPopularMoviesUseCase,
    getTrendingMoviesUseCase: GetTrendingMoviesUseCase,
    getUpcomingMoviesUseCase: GetUpcomingMoviesUseCase,
    getWatchHistoryUseCase: GetWatchHistoryUseCase,
    private val getDailyBoxOfficeWithTmdbMatchUseCase: GetDailyBoxOfficeWithTmdbMatchUseCase,
    private val getWeeklyBoxOfficeWithTmdbMatchUseCase: GetWeeklyBoxOfficeWithTmdbMatchUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 프로세스 종료 후에도 선택한 박스오피스 기간을 복원한다 (알 수 없는 값이면 기본 DAILY로 폴백)
    private val initialPeriod: BoxOfficePeriod =
        savedStateHandle.get<String>(KEY_BOX_OFFICE_PERIOD)
            ?.let { saved -> BoxOfficePeriod.entries.firstOrNull { it.name == saved } }
            ?: BoxOfficePeriod.DAILY

    private val _boxOfficeUiState = MutableStateFlow(BoxOfficeUiState(period = initialPeriod))
    val boxOfficeUiState: StateFlow<BoxOfficeUiState> = _boxOfficeUiState.asStateFlow()

    // 기간 전환 시 직전 로딩을 취소해 늦게 도착한 응답이 최신 상태를 덮어쓰지 않도록 한다
    private var boxOfficeJob: Job? = null

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

    init {
        loadBoxOffice(initialPeriod)
    }

    fun onTabSelected(tab: HomeTab) {
        _selectedTab.value = tab
    }

    // 동일 기간을 다시 선택하면(칩 재선택 등) 불필요한 재로딩을 하지 않는다
    fun onBoxOfficePeriodSelected(period: BoxOfficePeriod) {
        if (_boxOfficeUiState.value.period == period) return
        savedStateHandle[KEY_BOX_OFFICE_PERIOD] = period.name
        loadBoxOffice(period)
    }

    // 재시도는 항상 현재 선택된 기간을 대상으로 한다 (기간 하드코딩 금지)
    fun retryBoxOffice() {
        loadBoxOffice(_boxOfficeUiState.value.period)
    }

    private fun loadBoxOffice(period: BoxOfficePeriod) {
        boxOfficeJob?.cancel()
        _boxOfficeUiState.update {
            it.copy(period = period, isLoading = true, items = emptyList(), errorType = null)
        }
        boxOfficeJob = viewModelScope.launchWithErrorHandler(
            onError = { errorType ->
                _boxOfficeUiState.update { it.copy(isLoading = false, items = emptyList(), errorType = errorType) }
            }
        ) {
            val items = when (period) {
                BoxOfficePeriod.DAILY -> getDailyBoxOfficeWithTmdbMatchUseCase()
                BoxOfficePeriod.WEEKLY -> getWeeklyBoxOfficeWithTmdbMatchUseCase()
            }
            _boxOfficeUiState.update { it.copy(isLoading = false, items = items, errorType = null) }
        }
    }

    private companion object {
        const val KEY_BOX_OFFICE_PERIOD = "boxOfficePeriod"
    }
}
