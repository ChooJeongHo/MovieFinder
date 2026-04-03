package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.DailyWatchCount
import com.choo.moviefinder.domain.model.GenreCount
import com.choo.moviefinder.domain.model.MonthlyWatchCount
import com.choo.moviefinder.domain.model.Movie
import kotlinx.coroutines.flow.Flow

interface WatchHistoryRepository {

    // 시청 기록 목록을 Flow로 반환한다
    fun getWatchHistory(): Flow<List<Movie>>

    // 영화를 시청 기록에 저장한다
    suspend fun saveWatchHistory(movie: Movie)

    // 영화를 장르 정보와 함께 시청 기록에 저장한다
    suspend fun saveWatchHistoryWithGenres(movie: Movie, genres: String)

    // 모든 시청 기록을 삭제한다
    suspend fun clearWatchHistory()

    // 총 시청 편수를 Flow로 반환한다
    fun getTotalWatchedCount(): Flow<Int>

    // 특정 시점 이후 시청 편수를 Flow로 반환한다
    fun getWatchedCountSince(since: Long): Flow<Int>

    // 모든 시청 기록의 장르 문자열 목록을 Flow로 반환한다
    fun getAllWatchedGenres(): Flow<List<String>>

    // 정규화 테이블에서 장르별 시청 편수를 집계하여 반환한다
    fun getGenreCounts(): Flow<List<GenreCount>>

    // 월별 시청 편수를 Flow로 반환한다
    fun getMonthlyWatchCounts(): Flow<List<MonthlyWatchCount>>

    // 일별 시청 편수를 Flow로 반환한다 (since 이후 기록만 조회)
    fun getDailyWatchCounts(since: Long): Flow<List<DailyWatchCount>>
}
