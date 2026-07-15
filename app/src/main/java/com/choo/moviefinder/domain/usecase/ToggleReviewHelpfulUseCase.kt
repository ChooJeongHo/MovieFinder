package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.ReviewFeedbackRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class ToggleReviewHelpfulUseCase @Inject constructor(
    private val repository: ReviewFeedbackRepository
) {
    // 리뷰의 도움이 됨 표시를 켜거나 끈다
    suspend operator fun invoke(movieId: Int, reviewId: String, helpful: Boolean) =
        repository.setHelpful(movieId, reviewId, helpful)
}
