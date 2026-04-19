package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.PersonRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetPersonDetailUseCase @Inject constructor(
    private val repository: PersonRepository
) {
    // 인물 ID로 상세 정보를 조회한다
    suspend operator fun invoke(personId: Int) = repository.getPersonDetail(personId)
}
