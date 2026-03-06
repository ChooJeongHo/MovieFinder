package com.choo.moviefinder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.choo.moviefinder.data.local.entity.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSearchDao {

    // 최근 검색어 10개를 시간 역순으로 조회
    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<RecentSearchEntity>>

    // 검색어 삽입 (중복 시 타임스탬프 갱신)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(search: RecentSearchEntity)

    // 특정 검색어 삭제
    @Query("DELETE FROM recent_searches WHERE `query` = :query")
    suspend fun delete(query: String)

    // 모든 검색 기록 삭제
    @Query("DELETE FROM recent_searches")
    suspend fun clearAll()
}