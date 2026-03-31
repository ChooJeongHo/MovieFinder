package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.WatchHistoryRepository
import javax.inject.Inject

class GetWatchHistoryUseCase @Inject constructor(
    private val repository: WatchHistoryRepository
) {
    // 시청 기록 목록을 Flow로 조회한다
    operator fun invoke() = repository.getWatchHistory()
}
