package com.choo.moviefinder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.choo.moviefinder.data.local.entity.KoreanRatingCacheEntity

@Dao
interface KoreanRatingCacheDao {

    // 영화 제목(원문)으로 캐시된 KMRB 등급 조회 결과를 조회한다
    @Query("SELECT * FROM korean_rating_cache WHERE movieTitle = :movieTitle")
    suspend fun find(movieTitle: String): KoreanRatingCacheEntity?

    // KMRB 등급 조회 결과를 삽입하거나 교체한다
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: KoreanRatingCacheEntity)
}
