package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class SaveSearchQueryUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 검색어를 최근 검색 기록에 저장한다
    suspend operator fun invoke(query: String) = repository.saveSearchQuery(query)
}