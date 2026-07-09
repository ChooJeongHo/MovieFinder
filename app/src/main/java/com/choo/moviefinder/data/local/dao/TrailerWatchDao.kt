package com.choo.moviefinder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.choo.moviefinder.data.local.entity.TrailerWatchEntity

@Dao
interface TrailerWatchDao {

    // 특정 영화의 트레일러 시청 기록을 조회한다
    @Query("SELECT * FROM trailer_watches WHERE movieId = :movieId")
    suspend fun getTrailerWatch(movieId: Int): TrailerWatchEntity?

    // 트레일러 시청 기록을 삽입하거나 교체한다
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markWatched(entity: TrailerWatchEntity)
}
