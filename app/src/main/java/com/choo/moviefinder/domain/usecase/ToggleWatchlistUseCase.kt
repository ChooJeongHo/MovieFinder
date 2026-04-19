package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.WatchlistRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class ToggleWatchlistUseCase @Inject constructor(
    private val repository: WatchlistRepository
) {
    // 영화의 워치리스트 상태를 토글한다 (추가/삭제)
    suspend operator fun invoke(movie: Movie) = repository.toggleWatchlist(movie)
}
