package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.FavoriteRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class ToggleFavoriteUseCase @Inject constructor(
    private val repository: FavoriteRepository
) {
    // 영화의 즐겨찾기 상태를 토글한다 (추가/삭제)
    suspend operator fun invoke(movie: Movie) = repository.toggleFavorite(movie)
}