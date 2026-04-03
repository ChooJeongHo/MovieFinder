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

    // 월별 시청 편수를 최근 6개월 기준으로 조회
    @Query(
        "SELECT strftime('%Y-%m', watchedAt / 1000, 'unixepoch', 'localtime') AS yearMonth, " +
            "COUNT(*) AS count FROM watch_history " +
            "GROUP BY yearMonth ORDER BY yearMonth DESC LIMIT 6"
    )
    fun getMonthlyWatchCounts(): Flow<List<MonthlyCount>>

    // 일별 시청 편수를 조회하여 캘린더 히트맵 데이터를 반환한다 (since 이후 기록만 조회)
    @Query(
        "SELECT date(watchedAt / 1000, 'unixepoch', 'localtime') as date, " +
            "COUNT(*) as count FROM watch_history WHERE watchedAt >= :since GROUP BY date ORDER BY date ASC"
    )
    fun getDailyWatchCounts(since: Long): Flow<List<DailyCount>>
}

data class DailyCount(val date: String, val count: Int)
