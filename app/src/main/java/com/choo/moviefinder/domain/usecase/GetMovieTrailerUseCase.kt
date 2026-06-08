package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieDetailRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetMovieTrailerUseCase @Inject constructor(
    private val repository: MovieDetailRepository
) {
    // 영화 ID로 YouTube 예고편 키를 조회한다
    suspend operator fun invoke(movieId: Int): String? = repository.getMovieTrailerKey(movieId)
}
