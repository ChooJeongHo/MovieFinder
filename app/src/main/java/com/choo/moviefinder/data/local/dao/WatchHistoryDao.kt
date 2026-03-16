package com.choo.moviefinder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.choo.moviefinder.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {

    // 최근 시청 기록을 제한 개수만큼 시간 역순으로 조회
    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<WatchHistoryEntity>>

    // 시청 기록 삽입 (중복 시 타임스탬프 갱신)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WatchHistoryEntity)

    // 모든 시청 기록 삭제
    @Query("DELETE FROM watch_history")
    suspend fun clearAll()

    // 총 시청 편수 조회
    @Query("SELECT COUNT(*) FROM watch_history")
    fun getTotalCount(): Flow<Int>

    // 특정 시점 이후 시청 편수 조회
    @Query("SELECT COUNT(*) FROM watch_history WHERE watchedAt >= :since")
    fun getCountSince(since: Long): Flow<Int>

    // 모든 시청 기록의 장르 문자열 조회
    @Query("SELECT genres FROM watch_history WHERE genres != ''")
    fun getAllGenres(): Flow<List<String>>
}
