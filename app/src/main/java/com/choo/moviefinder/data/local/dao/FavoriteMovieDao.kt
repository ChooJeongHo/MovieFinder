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

    @Query("SELECT * FROM favorite_movies ORDER BY addedAt DESC")
    abstract fun getAllFavorites(): Flow<List<FavoriteMovieEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(movie: FavoriteMovieEntity)

    @Query("DELETE FROM favorite_movies WHERE id = :movieId")
    abstract suspend fun deleteById(movieId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_movies WHERE id = :movieId)")
    abstract fun isFavorite(movieId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_movies WHERE id = :movieId)")
    abstract suspend fun isFavoriteOnce(movieId: Int): Boolean

    @Transaction
    open suspend fun toggleFavorite(movie: FavoriteMovieEntity) {
        if (isFavoriteOnce(movie.id)) {
            deleteById(movie.id)
        } else {
            insert(movie)
        }
    }
}