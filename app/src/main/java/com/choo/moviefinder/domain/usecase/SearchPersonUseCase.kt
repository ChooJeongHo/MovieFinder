package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class SearchPersonUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 이름으로 배우/인물을 검색하여 결과 목록을 반환한다
    suspend operator fun invoke(query: String) = repository.searchPerson(query)
}
