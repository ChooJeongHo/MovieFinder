package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.core.util.currentMonthStartMillis
import com.choo.moviefinder.domain.model.GenreCount
import com.choo.moviefinder.domain.model.MonthlyWatchCount
import com.choo.moviefinder.domain.model.WatchStats
import com.choo.moviefinder.domain.repository.MovieRepository
import com.choo.moviefinder.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetWatchStatsUseCase @Inject constructor(
    private val repository: MovieRepository,
    private val preferencesRepository: PreferencesRepository
) {
    // 시청 통계 데이터를 여러 Flow를 combine하여 반환한다
    // flow { } 빌더로 감싸서 재수집 시마다 monthStartMillis를 새로 계산한다 (월 경계 넘김 대응)
    operator fun invoke(): Flow<WatchStats> = flow {
        val monthStartMillis = currentMonthStartMillis()

        val baseStatsFlow = combine(
            repository.getTotalWatchedCount(),
            repository.getWatchedCountSince(monthStartMillis),
            repository.getAverageUserRating(),
            repository.getAllWatchedGenres(),
            repository.getMonthlyWatchCounts()
        ) { total, monthly, avgRating, genreStrings, monthlyCounts ->
            BaseStats(total, monthly, avgRating, genreStrings, monthlyCounts)
        }

        val combinedFlow = combine(
            baseStatsFlow,
            preferencesRepository.getMonthlyWatchGoal(),
            repository.getRatingDistribution(),
            repository.getDailyWatchCounts()
        ) { base, watchGoal, ratingDist, dailyCounts ->
            val allGenres = computeAllGenres(base.genreStrings)
            WatchStats(
                totalWatched = base.total,
                monthlyWatched = base.monthly,
                averageRating = base.avgRating,
                topGenres = allGenres.take(3),
                allGenreCounts = allGenres,
                monthlyWatchCounts = base.monthlyCounts,
                monthlyWatchGoal = watchGoal,
                ratingDistribution = ratingDist,
                dailyWatchCounts = dailyCounts
            )
        }
        emitAll(combinedFlow)
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

    // 5개 기본 통계 Flow를 묶기 위한 내부 데이터 홀더
    private data class BaseStats(
        val total: Int,
        val monthly: Int,
        val avgRating: Float?,
        val genreStrings: List<String>,
        val monthlyCounts: List<MonthlyWatchCount>
    )
}
