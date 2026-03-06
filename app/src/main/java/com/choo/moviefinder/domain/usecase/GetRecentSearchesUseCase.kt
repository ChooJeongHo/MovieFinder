package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class GetRecentSearchesUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 최근 검색어 목록을 Flow로 조회한다
    operator fun invoke() = repository.getRecentSearches()
}