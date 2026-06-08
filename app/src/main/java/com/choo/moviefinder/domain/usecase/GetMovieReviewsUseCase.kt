package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieDetailRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetMovieReviewsUseCase @Inject constructor(
    private val repository: MovieDetailRepository
) {
    // 영화 ID로 사용자 리뷰 목록을 조회한다
    suspend operator fun invoke(movieId: Int) = repository.getMovieReviews(movieId)
}
