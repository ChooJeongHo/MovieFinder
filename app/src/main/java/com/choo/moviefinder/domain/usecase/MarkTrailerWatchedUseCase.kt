package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.TrailerWatchRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class MarkTrailerWatchedUseCase @Inject constructor(
    private val repository: TrailerWatchRepository
) {
    // 영화의 트레일러를 시청했다고 기록한다
    suspend operator fun invoke(movieId: Int, trailerKey: String) =
        repository.markTrailerWatched(movieId, trailerKey)
}
