package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieDetailRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetMovieCreditsUseCase @Inject constructor(
    private val repository: MovieDetailRepository
) {
    // 영화 ID로 출연진 목록을 조회한다
    suspend operator fun invoke(movieId: Int) = repository.getMovieCredits(movieId)
}