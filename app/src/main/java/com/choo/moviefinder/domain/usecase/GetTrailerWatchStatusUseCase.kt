package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.TrailerWatch
import com.choo.moviefinder.domain.repository.TrailerWatchRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetTrailerWatchStatusUseCase @Inject constructor(
    private val repository: TrailerWatchRepository
) {
    // 영화 ID로 트레일러 시청 기록을 조회한다 (없으면 null)
    suspend operator fun invoke(movieId: Int): TrailerWatch? = repository.getTrailerWatch(movieId)
}
