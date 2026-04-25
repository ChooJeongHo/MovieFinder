package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.WatchlistRepository
import javax.inject.Inject

class ClearWatchlistReminderUseCase @Inject constructor(
    private val watchlistRepository: WatchlistRepository
) {
    // 워치리스트 영화의 알림 날짜를 DB에서 삭제한다
    suspend operator fun invoke(movieId: Int) {
        watchlistRepository.clearReminder(movieId)
    }
}
