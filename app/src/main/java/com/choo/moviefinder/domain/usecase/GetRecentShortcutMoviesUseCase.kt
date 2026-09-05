package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.WatchHistoryRepository
import dagger.Reusable
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@Reusable
class GetRecentShortcutMoviesUseCase @Inject constructor(
    private val repository: WatchHistoryRepository
) {
    // 최근 시청한 영화 중 단축키로 노출할 목록을 반환한다.
    // WatchHistoryDao는 재시청 시 새 rowId를 발급해 같은 영화가 여러 번 쌓일 수 있으므로
    // distinctBy로 중복을 제거한 뒤 limit만큼만 취한다.
    suspend operator fun invoke(limit: Int = MAX_SHORTCUT_MOVIES): List<Movie> {
        return repository.getWatchHistory().first()
            .distinctBy { it.id }
            .take(limit)
    }

    companion object {
        const val MAX_SHORTCUT_MOVIES = 2
    }
}
