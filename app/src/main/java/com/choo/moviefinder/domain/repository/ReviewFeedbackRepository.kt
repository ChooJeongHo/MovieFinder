package com.choo.moviefinder.domain.repository

interface ReviewFeedbackRepository {

    // 특정 영화에서 도움이 됨으로 표시된 리뷰 ID 집합을 조회한다
    suspend fun getHelpfulReviewIds(movieId: Int): Set<String>

    // 리뷰의 도움이 됨 표시를 켜거나 끈다
    suspend fun setHelpful(movieId: Int, reviewId: String, helpful: Boolean)
}
