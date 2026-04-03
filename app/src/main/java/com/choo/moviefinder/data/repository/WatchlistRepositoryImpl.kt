package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.WatchlistDao
import com.choo.moviefinder.data.local.entity.toDomain
import com.choo.moviefinder.data.local.entity.toWatchlistEntity
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WatchlistRepositoryImpl @Inject constructor(
    private val watchlistDao: WatchlistDao
) : WatchlistRepository {

    // 워치리스트 영화 목록을 실시간 Flow로 조회
    override fun getWatchlistMovies(): Flow<List<Movie>> {
        return watchlistDao.getAllWatchlist().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // 워치리스트 상태 토글 (추가/삭제)
    override suspend fun toggleWatchlist(movie: Movie) {
        watchlistDao.toggleWatchlist(movie.toWatchlistEntity())
    }

    // 해당 영화의 워치리스트 여부를 실시간 관찰
    override fun isInWatchlist(movieId: Int): Flow<Boolean> {
        return watchlistDao.isInWatchlist(movieId)
    }
}
