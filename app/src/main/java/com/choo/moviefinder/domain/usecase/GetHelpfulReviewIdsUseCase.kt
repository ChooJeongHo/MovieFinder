package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.ReviewFeedbackRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetHelpfulReviewIdsUseCase @Inject constructor(
    private val repository: ReviewFeedbackRepository
) {
    // 특정 영화에서 도움이 됨으로 표시된 리뷰 ID 집합을 조회한다
    suspend operator fun invoke(movieId: Int): Set<String> = repository.getHelpfulReviewIds(movieId)
}
