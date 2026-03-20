package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.GenreCount
import com.choo.moviefinder.domain.model.MonthlyWatchCount
import com.choo.moviefinder.domain.model.WatchStats
import com.choo.moviefinder.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.atStartOfDayIn
import javax.inject.Inject

class GetWatchStatsUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 시청 통계 데이터를 5개 Flow를 combine하여 반환한다
    operator fun invoke(): Flow<WatchStats> {
        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        val localDate = now.toLocalDateTime(tz).date
        val monthStart = kotlinx.datetime.LocalDate(localDate.year, localDate.month, 1)
        val monthStartMillis = monthStart.atStartOfDayIn(tz).toEpochMilliseconds()

        return combine(
            repository.getTotalWatchedCount(),
            repository.getWatchedCountSince(monthStartMillis),
            repository.getAverageUserRating(),
            repository.getAllWatchedGenres(),
            repository.getMonthlyWatchCounts()
        ) { total, monthly, avgRating, genreStrings, monthlyCounts ->
            val allGenres = computeAllGenres(genreStrings)
            WatchStats(
                totalWatched = total,
                monthlyWatched = monthly,
                averageRating = avgRating,
                topGenres = allGenres.take(3),
                allGenreCounts = allGenres,
                monthlyWatchCounts = monthlyCounts.map {
                    MonthlyWatchCount(yearMonth = it.yearMonth, count = it.count)
                }
            )
        }
    }

    // 장르 문자열 목록에서 출현 빈도순으로 전체 장르 카운트를 계산한다
    private fun computeAllGenres(genreStrings: List<String>): List<GenreCount> {
        return genreStrings
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { GenreCount(name = it.key, count = it.value) }
    }
}
