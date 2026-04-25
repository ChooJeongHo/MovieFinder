package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.WatchlistRepository
import javax.inject.Inject

class SetWatchlistReminderUseCase @Inject constructor(
    private val watchlistRepository: WatchlistRepository
) {
    // 워치리스트 영화의 알림 날짜를 DB에 저장한다
    suspend operator fun invoke(movieId: Int, dateMillis: Long) {
        watchlistRepository.setReminder(movieId, dateMillis)
    }
}
