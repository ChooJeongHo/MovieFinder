package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetMovieCertificationUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 영화 ID로 콘텐츠 등급 정보를 조회한다
    suspend operator fun invoke(movieId: Int) = repository.getMovieCertification(movieId)
}
