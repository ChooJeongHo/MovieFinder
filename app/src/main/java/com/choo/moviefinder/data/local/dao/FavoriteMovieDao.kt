package com.choo.moviefinder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.choo.moviefinder.data.local.entity.FavoriteMovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FavoriteMovieDao {

    // 모든 즐겨찾기 영화를 추가 날짜 역순으로 조회
    @Query("SELECT * FROM favorite_movies ORDER BY addedAt DESC")
    abstract fun getAllFavorites(): Flow<List<FavoriteMovieEntity>>

    // 즐겨찾기 영화 삽입 (중복 시 교체)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(movie: FavoriteMovieEntity)

    // 영화 ID로 즐겨찾기 삭제
    @Query("DELETE FROM favorite_movies WHERE id = :movieId")
    abstract suspend fun deleteById(movieId: Int)

    // 해당 영화가 즐겨찾기인지 실시간 관찰
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_movies WHERE id = :movieId)")
    abstract fun isFavorite(movieId: Int): Flow<Boolean>

    // 해당 영화가 즐겨찾기인지 일회성 조회
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_movies WHERE id = :movieId)")
    abstract suspend fun isFavoriteOnce(movieId: Int): Boolean

    // 즐겨찾기 상태를 원자적으로 토글 (추가/삭제)
    @Transaction
    open suspend fun toggleFavorite(movie: FavoriteMovieEntity) {
        if (isFavoriteOnce(movie.id)) {
            deleteById(movie.id)
        } else {
            insert(movie)
        }
    }

    // 모든 즐겨찾기를 일회성으로 조회 (백업용)
    @Query("SELECT * FROM favorite_movies ORDER BY addedAt DESC")
    abstract suspend fun getAllFavoritesOnce(): List<FavoriteMovieEntity>

    // 여러 즐겨찾기를 한 번에 삽입 (복원용)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(movies: List<FavoriteMovieEntity>)
}