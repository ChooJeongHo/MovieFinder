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
}
