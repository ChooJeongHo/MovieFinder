package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class GetTrendingMoviesUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 일별 트렌딩 영화 목록을 페이징 데이터로 조회한다
    operator fun invoke() = repository.getTrendingMovies()
}
