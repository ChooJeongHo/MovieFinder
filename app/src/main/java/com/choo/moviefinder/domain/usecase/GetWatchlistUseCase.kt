package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class GetWatchlistUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 워치리스트(보고 싶은 영화) 목록을 Flow로 조회한다
    operator fun invoke() = repository.getWatchlistMovies()
}
