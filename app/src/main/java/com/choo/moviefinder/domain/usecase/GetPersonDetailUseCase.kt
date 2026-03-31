package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class GetPersonDetailUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 인물 ID로 상세 정보를 조회한다
    suspend operator fun invoke(personId: Int) = repository.getPersonDetail(personId)
}
