package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.FavoriteSortOrder
import com.choo.moviefinder.domain.model.Movie
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {

    // 즐겨찾기한 영화 목록을 Flow로 반환한다
    fun getFavoriteMovies(): Flow<List<Movie>>

    // 정렬 순서를 지정하여 즐겨찾기 목록을 Flow로 반환한다
    fun getFavoriteMoviesSorted(sortOrder: FavoriteSortOrder): Flow<List<Movie>>

    // 영화의 즐겨찾기 상태를 토글한다 (추가/삭제)
    suspend fun toggleFavorite(movie: Movie)

    // 영화 ID로 즐겨찾기 여부를 Flow로 반환한다
    fun isFavorite(movieId: Int): Flow<Boolean>
}
