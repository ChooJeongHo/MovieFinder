package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetUpcomingMoviesUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 개봉 예정 영화 목록을 페이징 데이터로 조회한다
    operator fun invoke() = repository.getUpcomingMovies()
}
