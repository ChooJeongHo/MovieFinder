package com.choo.moviefinder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.choo.moviefinder.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WatchlistDao {

    @Query("SELECT * FROM watchlist_movies ORDER BY addedAt DESC")
    abstract fun getAllWatchlist(): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: WatchlistEntity)

    @Query("DELETE FROM watchlist_movies WHERE id = :movieId")
    abstract suspend fun deleteById(movieId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist_movies WHERE id = :movieId)")
    abstract fun isInWatchlist(movieId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist_movies WHERE id = :movieId)")
    abstract suspend fun isInWatchlistOnce(movieId: Int): Boolean

    @Transaction
    open suspend fun toggleWatchlist(entity: WatchlistEntity) {
        if (isInWatchlistOnce(entity.id)) {
            deleteById(entity.id)
        } else {
            insert(entity)
        }
    }
}
