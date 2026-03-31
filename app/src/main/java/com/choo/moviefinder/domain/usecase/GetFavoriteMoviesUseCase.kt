package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.FavoriteRepository
import javax.inject.Inject

class GetFavoriteMoviesUseCase @Inject constructor(
    private val repository: FavoriteRepository
) {
    // 즐겨찾기한 영화 목록을 Flow로 조회한다
    operator fun invoke() = repository.getFavoriteMovies()
}