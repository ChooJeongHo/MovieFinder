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
}
