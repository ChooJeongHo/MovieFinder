package com.choo.moviefinder.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.choo.moviefinder.data.local.entity.CachedMovieEntity

@Dao
interface CachedMovieDao {

    @Query("SELECT * FROM cached_movies WHERE category = :category ORDER BY page ASC, cachedAt ASC")
    fun getMoviesByCategory(category: String): PagingSource<Int, CachedMovieEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movies: List<CachedMovieEntity>)

    @Query("DELETE FROM cached_movies WHERE category = :category")
    suspend fun clearByCategory(category: String)
}
