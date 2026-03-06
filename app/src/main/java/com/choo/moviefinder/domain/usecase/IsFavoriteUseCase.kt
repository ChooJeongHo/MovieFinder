package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class IsFavoriteUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 영화 ID로 즐겨찾기 여부를 Flow로 확인한다
    operator fun invoke(movieId: Int) = repository.isFavorite(movieId)
}