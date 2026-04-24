package com.choo.moviefinder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.choo.moviefinder.data.local.entity.UserRatingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserRatingDao {

    // 영화의 사용자 평점을 실시간 관찰 (없으면 null)
    @Query("SELECT rating FROM user_ratings WHERE movieId = :movieId")
    fun getRating(movieId: Int): Flow<Float?>

    // 사용자 평점 삽입 (기존 평점 덮어쓰기)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRating(entity: UserRatingEntity)

    // 영화의 사용자 평점 삭제
    @Query("DELETE FROM user_ratings WHERE movieId = :movieId")
    suspend fun deleteRating(movieId: Int)

    // 모든 사용자 평점의 평균 조회
    @Query("SELECT AVG(rating) FROM user_ratings")
    fun getAverageRating(): Flow<Float?>

    // 평점별 개수를 조회하여 분포 데이터를 반환한다
    @Query("SELECT rating, COUNT(*) as count FROM user_ratings GROUP BY rating ORDER BY rating ASC")
    fun getRatingDistribution(): Flow<List<RatingCount>>

    // 모든 사용자 평점을 일회성으로 조회 (백업용)
    @Query("SELECT * FROM user_ratings")
    suspend fun getAllRatings(): List<UserRatingEntity>

    // 모든 사용자 평점을 실시간 Flow로 조회 (즐겨찾기 평점 필터용)
    @Query("SELECT * FROM user_ratings")
    fun getAllRatingsFlow(): Flow<List<UserRatingEntity>>

    // 여러 평점을 한 번에 삽입 (복원용)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ratings: List<UserRatingEntity>)
}

data class RatingCount(val rating: Float, val count: Int)
