package com.choo.moviefinder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.choo.moviefinder.data.local.entity.MovieKoreanTitleCacheEntity

@Dao
interface MovieKoreanTitleCacheDao {

    // movieId로 캐시된 한국어 제목을 조회한다 (제목은 바뀌지 않는 값이라 TTL 없이 영구 신뢰)
    @Query("SELECT * FROM movie_korean_title_cache WHERE movieId = :movieId")
    suspend fun find(movieId: Int): MovieKoreanTitleCacheEntity?

    // 한국어 제목 조회 결과를 삽입하거나 교체한다
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MovieKoreanTitleCacheEntity)
}
