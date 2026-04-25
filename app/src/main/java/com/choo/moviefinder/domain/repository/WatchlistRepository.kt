package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.FavoriteSortOrder
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.WatchlistReminder
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {

    // 워치리스트 영화 목록을 Flow로 반환한다
    fun getWatchlistMovies(): Flow<List<Movie>>

    // 정렬 순서를 지정하여 워치리스트 목록을 Flow로 반환한다
    fun getWatchlistMoviesSorted(sortOrder: FavoriteSortOrder): Flow<List<Movie>>

    // 영화의 워치리스트 상태를 토글한다 (추가/삭제)
    suspend fun toggleWatchlist(movie: Movie)

    // 영화 ID로 워치리스트 포함 여부를 Flow로 반환한다
    fun isInWatchlist(movieId: Int): Flow<Boolean>

    // 제목으로 워치리스트 영화를 검색한다 (오프라인 검색용)
    suspend fun searchWatchlistMovies(query: String): List<Movie>

    // 워치리스트 영화의 알림 날짜를 설정한다
    suspend fun setReminder(movieId: Int, dateMillis: Long)

    // 워치리스트 영화의 알림을 삭제한다
    suspend fun clearReminder(movieId: Int)

    // 알림 날짜가 설정된 워치리스트 영화 목록을 반환한다
    suspend fun getMoviesWithReminder(): List<WatchlistReminder>

    // 알림 날짜가 설정된 워치리스트 영화 목록을 실시간 Flow로 관찰한다
    fun observeMoviesWithReminder(): Flow<List<WatchlistReminder>>
}
