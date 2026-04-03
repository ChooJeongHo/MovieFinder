package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.FavoriteMovieDao
import com.choo.moviefinder.data.local.entity.toDomain
import com.choo.moviefinder.data.local.entity.toEntity
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteMovieDao: FavoriteMovieDao
) : FavoriteRepository {

    // 즐겨찾기 영화 목록을 실시간 Flow로 조회
    override fun getFavoriteMovies(): Flow<List<Movie>> {
        return favoriteMovieDao.getAllFavorites().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // 즐겨찾기 상태 토글 (추가/삭제)
    override suspend fun toggleFavorite(movie: Movie) {
        favoriteMovieDao.toggleFavorite(movie.toEntity())
    }

    // 해당 영화의 즐겨찾기 여부를 실시간 관찰
    override fun isFavorite(movieId: Int): Flow<Boolean> {
        return favoriteMovieDao.isFavorite(movieId)
    }
}
