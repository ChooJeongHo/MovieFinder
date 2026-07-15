package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.HelpfulReviewDao
import com.choo.moviefinder.data.local.entity.HelpfulReviewEntity
import com.choo.moviefinder.domain.repository.ReviewFeedbackRepository
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ReviewFeedbackRepositoryImpl @Inject constructor(
    private val helpfulReviewDao: HelpfulReviewDao
) : ReviewFeedbackRepository {

    // 특정 영화에서 도움이 됨으로 표시된 리뷰 ID 집합을 Room DB에서 조회한다
    override suspend fun getHelpfulReviewIds(movieId: Int): Set<String> {
        return helpfulReviewDao.getHelpfulReviewIds(movieId).toSet()
    }

    // 리뷰의 도움이 됨 표시를 Room DB에 반영한다
    @OptIn(ExperimentalTime::class)
    override suspend fun setHelpful(movieId: Int, reviewId: String, helpful: Boolean) {
        if (helpful) {
            helpfulReviewDao.mark(
                HelpfulReviewEntity(
                    reviewId = reviewId,
                    movieId = movieId,
                    markedAt = Clock.System.now().toEpochMilliseconds()
                )
            )
        } else {
            helpfulReviewDao.unmark(reviewId)
        }
    }
}
