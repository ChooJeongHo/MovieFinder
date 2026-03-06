package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class ClearWatchHistoryUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 모든 시청 기록을 삭제한다
    suspend operator fun invoke() = repository.clearWatchHistory()
}
