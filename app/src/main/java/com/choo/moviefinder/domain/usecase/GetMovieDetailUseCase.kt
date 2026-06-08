package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieDetailRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetMovieDetailUseCase @Inject constructor(
    private val repository: MovieDetailRepository
) {
    // 영화 ID로 상세 정보를 조회한다
    suspend operator fun invoke(movieId: Int) = repository.getMovieDetail(movieId)
}