package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class GetMovieRecommendationsUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 영화 ID로 추천 영화 목록을 조회한다
    suspend operator fun invoke(movieId: Int) = repository.getMovieRecommendations(movieId)
}
