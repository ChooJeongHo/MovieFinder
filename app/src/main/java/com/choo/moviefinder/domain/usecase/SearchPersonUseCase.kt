package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.PersonRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class SearchPersonUseCase @Inject constructor(
    private val repository: PersonRepository
) {
    // 이름으로 배우/인물을 검색하여 결과 목록을 반환한다
    suspend operator fun invoke(query: String): List<com.choo.moviefinder.domain.model.PersonSearchItem> {
        require(query.isNotBlank()) { "Search query must not be blank" }
        return repository.searchPerson(query)
    }
}
