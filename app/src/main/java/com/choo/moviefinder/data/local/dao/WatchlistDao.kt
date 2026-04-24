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

    // 모든 워치리스트 영화를 추가 날짜 역순으로 조회
    @Query("SELECT * FROM watchlist_movies ORDER BY addedAt DESC")
    abstract fun getAllWatchlist(): Flow<List<WatchlistEntity>>

    // 제목 오름차순으로 워치리스트 목록 조회
    @Query("SELECT * FROM watchlist_movies ORDER BY title ASC")
    abstract fun getAllWatchlistSortedByTitle(): Flow<List<WatchlistEntity>>

    // 평점 내림차순으로 워치리스트 목록 조회
    @Query("SELECT * FROM watchlist_movies ORDER BY voteAverage DESC")
    abstract fun getAllWatchlistSortedByRating(): Flow<List<WatchlistEntity>>

    // 워치리스트 영화 삽입 (중복 시 교체)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: WatchlistEntity)

    // 영화 ID로 워치리스트에서 삭제
    @Query("DELETE FROM watchlist_movies WHERE id = :movieId")
    abstract suspend fun deleteById(movieId: Int)

    // 해당 영화가 워치리스트에 있는지 실시간 관찰
    @Query("SELECT EXISTS(SELECT 1 FROM watchlist_movies WHERE id = :movieId)")
    abstract fun isInWatchlist(movieId: Int): Flow<Boolean>

    // 해당 영화가 워치리스트에 있는지 일회성 조회
    @Query("SELECT EXISTS(SELECT 1 FROM watchlist_movies WHERE id = :movieId)")
    abstract suspend fun isInWatchlistOnce(movieId: Int): Boolean

    // 워치리스트 상태를 원자적으로 토글 (추가/삭제)
    @Transaction
    open suspend fun toggleWatchlist(entity: WatchlistEntity) {
        if (isInWatchlistOnce(entity.id)) {
            deleteById(entity.id)
        } else {
            insert(entity)
        }
    }

    // 제목으로 워치리스트 영화를 검색 (오프라인 검색용)
    @Query("SELECT * FROM watchlist_movies WHERE title LIKE '%' || :query || '%' ORDER BY addedAt DESC")
    abstract suspend fun searchWatchlist(query: String): List<WatchlistEntity>

    // 모든 워치리스트를 일회성으로 조회 (백업용)
    @Query("SELECT * FROM watchlist_movies ORDER BY addedAt DESC")
    abstract suspend fun getAllWatchlistOnce(): List<WatchlistEntity>

    // 여러 워치리스트 항목을 한 번에 삽입 (복원용)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(entities: List<WatchlistEntity>)

    // 워치리스트 영화의 알림 날짜를 설정한다
    @Query("UPDATE watchlist_movies SET reminderDate = :dateMillis WHERE id = :movieId")
    abstract suspend fun setReminder(movieId: Int, dateMillis: Long)

    // 워치리스트 영화의 알림을 삭제한다
    @Query("UPDATE watchlist_movies SET reminderDate = NULL WHERE id = :movieId")
    abstract suspend fun clearReminder(movieId: Int)

    // 알림 날짜가 설정된 워치리스트 영화를 조회한다
    @Query("SELECT * FROM watchlist_movies WHERE reminderDate IS NOT NULL ORDER BY reminderDate ASC")
    abstract suspend fun getMoviesWithReminder(): List<WatchlistEntity>
}
