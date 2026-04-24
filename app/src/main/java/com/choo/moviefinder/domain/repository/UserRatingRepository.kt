package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.RatingBucket
import kotlinx.coroutines.flow.Flow

interface UserRatingRepository {

    // 영화 ID로 사용자가 매긴 평점을 Flow로 반환한다
    fun getUserRating(movieId: Int): Flow<Float?>

    // 영화에 사용자 평점을 설정하여 저장한다
    suspend fun setUserRating(movieId: Int, rating: Float)

    // 영화에 매긴 사용자 평점을 삭제한다
    suspend fun deleteUserRating(movieId: Int)

    // 모든 사용자 평점의 평균을 Flow로 반환한다
    fun getAverageUserRating(): Flow<Float?>

    // 평점별 개수 분포를 Flow로 반환한다
    fun getRatingDistribution(): Flow<List<RatingBucket>>

    // 모든 사용자 평점을 실시간 Flow로 반환한다 (즐겨찾기 평점 필터용)
    fun getAllUserRatings(): Flow<Map<Int, Float>>
}
