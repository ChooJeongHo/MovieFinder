package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.SearchHistoryRepository
import javax.inject.Inject

class GetRecentSearchesUseCase @Inject constructor(
    private val repository: SearchHistoryRepository
) {
    // 최근 검색어 목록을 Flow로 조회한다
    operator fun invoke() = repository.getRecentSearches()
}