package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class DiscoverMoviesUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 장르, 정렬, 연도 필터로 영화를 탐색하여 페이징 데이터로 반환한다
    operator fun invoke(
        genres: Set<Int> = emptySet(),
        sortBy: String = "popularity.desc",
        year: Int? = null
    ) = repository.discoverMovies(genres, sortBy, year)
}
