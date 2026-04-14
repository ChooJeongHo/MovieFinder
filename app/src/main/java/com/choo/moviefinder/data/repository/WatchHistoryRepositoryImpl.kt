package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.WatchHistoryDao
import com.choo.moviefinder.data.local.entity.WatchHistoryGenreEntity
import com.choo.moviefinder.data.local.entity.toDomain
import com.choo.moviefinder.data.local.entity.toWatchHistoryEntity
import com.choo.moviefinder.data.util.Constants
import com.choo.moviefinder.domain.model.DailyWatchCount
import com.choo.moviefinder.domain.model.GenreCount
import com.choo.moviefinder.domain.model.MonthlyWatchCount
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.WatchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WatchHistoryRepositoryImpl @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao
) : WatchHistoryRepository {

    // 최근 시청 기록을 도메인 모델로 변환하여 조회
    override fun getWatchHistory(): Flow<List<Movie>> {
        return watchHistoryDao.getRecentHistory(Constants.WATCH_HISTORY_LIMIT).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // 영화를 시청 기록에 저장
    override suspend fun saveWatchHistory(movie: Movie) {
        watchHistoryDao.insert(movie.toWatchHistoryEntity())
    }

    // 영화를 장르 정보와 함께 시청 기록에 저장 — insert + insertGenres를 단일 트랜잭션으로 처리
    override suspend fun saveWatchHistoryWithGenres(movie: Movie, genres: String) {
        val genreEntities = genres.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { WatchHistoryGenreEntity(watchHistoryId = movie.id, genreName = it) }
        watchHistoryDao.insertWithGenres(movie.toWatchHistoryEntity(genres), genreEntities)
    }

    // 모든 시청 기록과 장르 기록을 단일 트랜잭션으로 원자적 삭제 (고아 데이터 방지)
    override suspend fun clearWatchHistory() {
        watchHistoryDao.clearAllWithGenres()
    }

    // 총 시청 편수를 실시간 Flow로 조회
    override fun getTotalWatchedCount(): Flow<Int> {
        return watchHistoryDao.getTotalCount()
    }

    // 특정 시점 이후 시청 편수를 실시간 Flow로 조회
    override fun getWatchedCountSince(since: Long): Flow<Int> {
        return watchHistoryDao.getCountSince(since)
    }

    // 모든 시청 기록의 장르 문자열 목록을 실시간 Flow로 조회
    override fun getAllWatchedGenres(): Flow<List<String>> {
        return watchHistoryDao.getAllGenres()
    }

    // 정규화 테이블에서 장르별 시청 편수를 도메인 모델로 변환하여 조회
    override fun getGenreCounts(): Flow<List<GenreCount>> {
        return watchHistoryDao.getGenreCounts().map { results ->
            results.map { GenreCount(name = it.genre, count = it.count) }
        }
    }

    // 월별 시청 편수를 도메인 모델로 변환하여 조회
    override fun getMonthlyWatchCounts(): Flow<List<MonthlyWatchCount>> {
        return watchHistoryDao.getMonthlyWatchCounts().map { counts ->
            counts.map { MonthlyWatchCount(yearMonth = it.yearMonth, count = it.count) }
        }
    }

    // 일별 시청 편수를 도메인 모델로 변환하여 조회 (since 이후 기록만)
    override fun getDailyWatchCounts(since: Long): Flow<List<DailyWatchCount>> {
        return watchHistoryDao.getDailyWatchCounts(since).map { counts ->
            counts.map { DailyWatchCount(date = it.date, count = it.count) }
        }
    }
}
