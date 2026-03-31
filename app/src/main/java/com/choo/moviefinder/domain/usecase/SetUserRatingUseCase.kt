package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.UserRatingRepository
import javax.inject.Inject

class SetUserRatingUseCase @Inject constructor(
    private val repository: UserRatingRepository
) {
    // 영화에 사용자 평점을 설정하여 저장한다
    suspend operator fun invoke(movieId: Int, rating: Float) =
        repository.setUserRating(movieId, rating)
}
