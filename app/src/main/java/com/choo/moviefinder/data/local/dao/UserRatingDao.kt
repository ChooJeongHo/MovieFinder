package com.choo.moviefinder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.choo.moviefinder.data.local.entity.UserRatingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserRatingDao {

    @Query("SELECT rating FROM user_ratings WHERE movieId = :movieId")
    fun getRating(movieId: Int): Flow<Float?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRating(entity: UserRatingEntity)

    @Query("DELETE FROM user_ratings WHERE movieId = :movieId")
    suspend fun deleteRating(movieId: Int)
}
