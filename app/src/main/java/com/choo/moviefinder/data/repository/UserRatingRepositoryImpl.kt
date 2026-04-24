package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.UserRatingDao
import com.choo.moviefinder.data.local.entity.UserRatingEntity
import com.choo.moviefinder.domain.model.RatingBucket
import com.choo.moviefinder.domain.repository.UserRatingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRatingRepositoryImpl @Inject constructor(
    private val userRatingDao: UserRatingDao
) : UserRatingRepository {

    // 영화의 사용자 평점을 실시간 Flow로 조회
    override fun getUserRating(movieId: Int): Flow<Float?> {
        return userRatingDao.getRating(movieId)
    }

    // 사용자 영화 평점 저장 (0.5~5.0 범위 검증)
    override suspend fun setUserRating(movieId: Int, rating: Float) {
        require(movieId > 0) { "Movie ID must be positive" }
        require(rating in 0.5f..5.0f) { "Rating must be between 0.5 and 5.0" }
        userRatingDao.insertRating(UserRatingEntity(movieId = movieId, rating = rating))
    }

    // 사용자 영화 평점 삭제
    override suspend fun deleteUserRating(movieId: Int) {
        userRatingDao.deleteRating(movieId)
    }

    // 모든 사용자 평점의 평균을 실시간 Flow로 조회
    override fun getAverageUserRating(): Flow<Float?> {
        return userRatingDao.getAverageRating()
    }

    // 평점별 개수 분포를 도메인 모델로 변환하여 조회
    override fun getRatingDistribution(): Flow<List<RatingBucket>> {
        return userRatingDao.getRatingDistribution().map { counts ->
            counts.map { RatingBucket(rating = it.rating, count = it.count) }
        }
    }

    // 모든 사용자 평점을 movieId → rating 맵으로 변환하여 실시간 Flow로 조회
    override fun getAllUserRatings(): Flow<Map<Int, Float>> {
        return userRatingDao.getAllRatingsFlow().map { entities ->
            entities.associate { it.movieId to it.rating }
        }
    }
}
