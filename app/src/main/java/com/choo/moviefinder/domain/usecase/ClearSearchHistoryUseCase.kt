package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.SearchHistoryRepository
import javax.inject.Inject

class ClearSearchHistoryUseCase @Inject constructor(
    private val repository: SearchHistoryRepository
) {
    // 모든 최근 검색 기록을 삭제한다
    suspend operator fun invoke() = repository.clearSearchHistory()
}