package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class SearchMoviesUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 검색어와 연도 필터로 영화를 검색하여 페이징 데이터로 반환한다
    operator fun invoke(query: String, year: Int? = null) = repository.searchMovies(query, year)
}