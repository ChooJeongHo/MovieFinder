package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.UserRatingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserRatingUseCase @Inject constructor(
    private val repository: UserRatingRepository
) {
    // 영화 ID로 사용자가 매긴 평점을 Flow로 조회한다
    operator fun invoke(movieId: Int): Flow<Float?> = repository.getUserRating(movieId)
}
