package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.FavoriteSortOrder
import com.choo.moviefinder.domain.model.Movie
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
}
