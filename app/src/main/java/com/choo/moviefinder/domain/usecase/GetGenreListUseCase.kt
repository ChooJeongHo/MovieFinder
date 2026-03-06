package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class GetGenreListUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 영화 장르 목록을 조회한다
    suspend operator fun invoke() = repository.getGenreList()
}
