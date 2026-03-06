package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class GetPopularMoviesUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 인기 영화 목록을 페이징 데이터로 조회한다
    operator fun invoke() = repository.getPopularMovies()
}