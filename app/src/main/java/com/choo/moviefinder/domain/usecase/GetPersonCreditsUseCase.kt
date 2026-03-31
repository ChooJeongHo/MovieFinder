package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.PersonRepository
import javax.inject.Inject

class GetPersonCreditsUseCase @Inject constructor(
    private val repository: PersonRepository
) {
    // 인물 ID로 출연 영화 목록을 조회한다
    suspend operator fun invoke(personId: Int) = repository.getPersonMovieCredits(personId)
}
