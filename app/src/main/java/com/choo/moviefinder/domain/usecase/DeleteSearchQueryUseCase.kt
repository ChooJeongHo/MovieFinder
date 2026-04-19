package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.SearchHistoryRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class DeleteSearchQueryUseCase @Inject constructor(
    private val repository: SearchHistoryRepository
) {
    // 특정 검색어를 최근 검색 기록에서 삭제한다
    suspend operator fun invoke(query: String) = repository.deleteSearchQuery(query)
}