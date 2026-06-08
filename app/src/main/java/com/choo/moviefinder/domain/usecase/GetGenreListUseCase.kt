package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieQueryRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetGenreListUseCase @Inject constructor(
    private val repository: MovieQueryRepository
) {
    // 영화 장르 목록을 조회한다
    suspend operator fun invoke() = repository.getGenreList()
}
