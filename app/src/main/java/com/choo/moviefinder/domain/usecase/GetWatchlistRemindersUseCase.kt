package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.WatchlistReminder
import com.choo.moviefinder.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetWatchlistRemindersUseCase @Inject constructor(
    private val watchlistRepository: WatchlistRepository
) {
    // 알림 날짜가 설정된 워치리스트 영화 목록을 반환한다
    suspend operator fun invoke(): List<WatchlistReminder> =
        watchlistRepository.getMoviesWithReminder()

    // 알림 날짜가 설정된 워치리스트 영화 목록을 실시간 Flow로 반환한다
    fun asFlow(): Flow<List<WatchlistReminder>> =
        watchlistRepository.observeMoviesWithReminder()
}
