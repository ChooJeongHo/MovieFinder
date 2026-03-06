package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class GetMovieTrailerUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 영화 ID로 YouTube 예고편 키를 조회한다
    suspend operator fun invoke(movieId: Int): String? = repository.getMovieTrailerKey(movieId)
}
