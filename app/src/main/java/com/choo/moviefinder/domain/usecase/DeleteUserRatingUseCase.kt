package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.UserRatingRepository
import javax.inject.Inject

class DeleteUserRatingUseCase @Inject constructor(
    private val repository: UserRatingRepository
) {
    // 영화에 매긴 사용자 평점을 삭제한다
    suspend operator fun invoke(movieId: Int) = repository.deleteUserRating(movieId)
}
