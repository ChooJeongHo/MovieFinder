package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.FavoriteSortOrder
import com.choo.moviefinder.domain.repository.WatchlistRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetWatchlistUseCase @Inject constructor(
    private val repository: WatchlistRepository
) {
    // 워치리스트(보고 싶은 영화) 목록을 Flow로 조회한다 (기본: 추가일 역순)
    operator fun invoke() = repository.getWatchlistMovies()

    // 정렬 순서를 지정하여 워치리스트 목록을 DB ORDER BY로 조회한다
    operator fun invoke(sortOrder: FavoriteSortOrder) = repository.getWatchlistMoviesSorted(sortOrder)
}
