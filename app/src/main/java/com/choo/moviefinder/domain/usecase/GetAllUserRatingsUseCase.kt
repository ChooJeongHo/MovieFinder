package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.UserRatingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllUserRatingsUseCase @Inject constructor(
    private val repository: UserRatingRepository
) {
    operator fun invoke(): Flow<Map<Int, Float>> = repository.getAllUserRatings()
}
