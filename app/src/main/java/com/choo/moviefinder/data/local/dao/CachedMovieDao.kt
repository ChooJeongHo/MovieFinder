package com.choo.moviefinder.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.choo.moviefinder.data.local.entity.CachedMovieEntity

@Dao
interface CachedMovieDao {

    // 카테고리별 캐시된 영화를 페이지 순으로 조회 (PagingSource 반환)
    @Query("SELECT * FROM cached_movies WHERE category = :category ORDER BY page ASC, cachedAt ASC")
    fun getMoviesByCategory(category: String): PagingSource<Int, CachedMovieEntity>

    // 캐시 영화 목록 일괄 삽입 (중복 시 교체)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movies: List<CachedMovieEntity>)

    // 특정 카테고리의 캐시 영화 전체 삭제
    @Query("DELETE FROM cached_movies WHERE category = :category")
    suspend fun clearByCategory(category: String)
}
