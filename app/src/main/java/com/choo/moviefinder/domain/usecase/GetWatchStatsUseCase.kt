package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.GenreCount
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
    // 시청 통계 데이터를 4개 Flow를 combine하여 반환한다
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
            repository.getAllWatchedGenres()
        ) { total, monthly, avgRating, genreStrings ->
            WatchStats(
                totalWatched = total,
                monthlyWatched = monthly,
                averageRating = avgRating,
                topGenres = computeTopGenres(genreStrings)
            )
        }
    }

    // 장르 문자열 목록에서 출현 빈도 Top 3를 계산한다
    private fun computeTopGenres(genreStrings: List<String>): List<GenreCount> {
        return genreStrings
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { GenreCount(name = it.key, count = it.value) }
    }
}
