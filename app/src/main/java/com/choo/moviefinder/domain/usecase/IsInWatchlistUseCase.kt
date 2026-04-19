package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.WatchlistRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class IsInWatchlistUseCase @Inject constructor(
    private val repository: WatchlistRepository
) {
    // 영화 ID로 워치리스트 포함 여부를 Flow로 확인한다
    operator fun invoke(movieId: Int) = repository.isInWatchlist(movieId)
}
