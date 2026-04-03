package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.FavoriteSortOrder
import com.choo.moviefinder.domain.repository.FavoriteRepository
import javax.inject.Inject

class GetFavoriteMoviesUseCase @Inject constructor(
    private val repository: FavoriteRepository
) {
    // 즐겨찾기한 영화 목록을 Flow로 조회한다 (기본: 추가일 역순)
    operator fun invoke() = repository.getFavoriteMovies()

    // 정렬 순서를 지정하여 즐겨찾기 목록을 DB ORDER BY로 조회한다
    operator fun invoke(sortOrder: FavoriteSortOrder) = repository.getFavoriteMoviesSorted(sortOrder)
}
