package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.WatchlistDao
import com.choo.moviefinder.data.local.entity.toDomain
import com.choo.moviefinder.data.local.entity.toWatchlistEntity
import com.choo.moviefinder.domain.model.FavoriteSortOrder
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WatchlistRepositoryImpl @Inject constructor(
    private val watchlistDao: WatchlistDao
) : WatchlistRepository {

    // 워치리스트 영화 목록을 실시간 Flow로 조회 (추가일 역순)
    override fun getWatchlistMovies(): Flow<List<Movie>> {
        return watchlistDao.getAllWatchlist().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // 정렬 순서를 지정하여 워치리스트 목록을 DB ORDER BY로 조회
    override fun getWatchlistMoviesSorted(sortOrder: FavoriteSortOrder): Flow<List<Movie>> {
        val raw = when (sortOrder) {
            FavoriteSortOrder.ADDED_DATE -> watchlistDao.getAllWatchlist()
            FavoriteSortOrder.TITLE -> watchlistDao.getAllWatchlistSortedByTitle()
            FavoriteSortOrder.RATING -> watchlistDao.getAllWatchlistSortedByRating()
        }
        return raw.map { entities -> entities.map { it.toDomain() } }
    }

    // 워치리스트 상태 토글 (추가/삭제)
    override suspend fun toggleWatchlist(movie: Movie) {
        watchlistDao.toggleWatchlist(movie.toWatchlistEntity())
    }

    // 해당 영화의 워치리스트 여부를 실시간 관찰
    override fun isInWatchlist(movieId: Int): Flow<Boolean> {
        return watchlistDao.isInWatchlist(movieId)
    }

    // 제목으로 워치리스트 영화를 검색 (오프라인 검색용)
    override suspend fun searchWatchlistMovies(query: String): List<Movie> {
        return watchlistDao.searchWatchlist(query).map { it.toDomain() }
    }
}
