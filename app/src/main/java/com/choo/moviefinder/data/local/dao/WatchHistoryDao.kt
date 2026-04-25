package com.choo.moviefinder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.choo.moviefinder.data.local.entity.WatchHistoryEntity
import com.choo.moviefinder.data.local.entity.WatchHistoryGenreEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WatchHistoryDao {

    // 최근 시청 기록을 제한 개수만큼 시간 역순으로 조회
    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC LIMIT :limit")
    abstract fun getRecentHistory(limit: Int): Flow<List<WatchHistoryEntity>>

    // 시청 기록 삽입 (재시청 지원 — 새 rowId 발급, 기존 레코드 유지)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(entity: WatchHistoryEntity): Long

    // 모든 시청 기록 삭제
    @Query("DELETE FROM watch_history")
    abstract suspend fun clearAll()

    // 모든 시청 장르 기록 삭제
    @Query("DELETE FROM watch_history_genre")
    abstract suspend fun clearAllGenres()

    // 총 시청 편수 조회
    @Query("SELECT COUNT(*) FROM watch_history")
    abstract fun getTotalCount(): Flow<Int>

    // 특정 시점 이후 시청 편수 조회
    @Query("SELECT COUNT(*) FROM watch_history WHERE watchedAt >= :since")
    abstract fun getCountSince(since: Long): Flow<Int>

    // 장르 엔티티 목록을 일괄 삽입한다 (중복 시 무시)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertGenres(genres: List<WatchHistoryGenreEntity>)

    // 정규화 테이블에서 장르별 시청 편수를 집계하여 조회한다
    @Query(
        "SELECT genre_name AS genre, COUNT(*) AS count " +
            "FROM watch_history_genre " +
            "GROUP BY genre_name ORDER BY count DESC"
    )
    abstract fun getGenreCounts(): Flow<List<GenreCountResult>>

    // 월별 시청 편수를 최근 6개월 기준으로 조회 (yearMonth 인덱스 활용)
    @Query(
        "SELECT yearMonth, COUNT(*) AS count FROM watch_history " +
            "GROUP BY yearMonth ORDER BY yearMonth DESC LIMIT 6"
    )
    abstract fun getMonthlyWatchCounts(): Flow<List<MonthlyCount>>

    // 일별 시청 편수를 조회하여 캘린더 히트맵 데이터를 반환한다 (since 이후 기록만 조회)
    @Query(
        "SELECT date(watchedAt / 1000, 'unixepoch', 'localtime') as date, " +
            "COUNT(*) as count FROM watch_history WHERE watchedAt >= :since GROUP BY date ORDER BY date ASC"
    )
    abstract fun getDailyWatchCounts(since: Long): Flow<List<DailyCount>>

    // 시청 기록과 장르를 하나의 트랜잭션으로 원자적 삽입
    // insert()가 반환한 rowId를 장르 엔티티의 watchHistoryId로 사용하여 FK 정합성 보장
    @Transaction
    open suspend fun insertWithGenres(entity: WatchHistoryEntity, genres: List<WatchHistoryGenreEntity>) {
        val rowId = insert(entity)
        if (genres.isNotEmpty()) {
            val linkedGenres = genres.map { it.copy(watchHistoryId = rowId) }
            insertGenres(linkedGenres)
        }
    }

    // 시청 기록 전체와 장르 기록을 하나의 트랜잭션으로 원자적 삭제 (고아 데이터 방지)
    @Transaction
    open suspend fun clearAllWithGenres() {
        clearAll()
        clearAllGenres()
    }
}

data class DailyCount(val date: String, val count: Int)
