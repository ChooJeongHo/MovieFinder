package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.core.util.currentMonthStartMillis
import com.choo.moviefinder.domain.model.GenreCount
import com.choo.moviefinder.domain.model.MonthlyWatchCount
import com.choo.moviefinder.domain.model.WatchStats
import com.choo.moviefinder.domain.repository.PreferencesRepository
import com.choo.moviefinder.domain.repository.UserRatingRepository
import com.choo.moviefinder.domain.repository.WatchHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetWatchStatsUseCase @Inject constructor(
    private val watchHistoryRepository: WatchHistoryRepository,
    private val userRatingRepository: UserRatingRepository,
    private val preferencesRepository: PreferencesRepository
) {
    // 시청 통계 데이터를 여러 Flow를 combine하여 반환한다
    // flow { } 빌더로 감싸서 재수집 시마다 monthStartMillis를 새로 계산한다 (월 경계 넘김 대응)
    operator fun invoke(): Flow<WatchStats> = flow {
        val monthStartMillis = currentMonthStartMillis()
        val threeMonthsAgoMillis = System.currentTimeMillis() - THREE_MONTHS_MILLIS

        val baseStatsFlow = combine(
            watchHistoryRepository.getTotalWatchedCount().distinctUntilChanged(),
            watchHistoryRepository.getWatchedCountSince(monthStartMillis).distinctUntilChanged(),
            userRatingRepository.getAverageUserRating().distinctUntilChanged(),
            watchHistoryRepository.getAllWatchedGenres().distinctUntilChanged(),
            watchHistoryRepository.getMonthlyWatchCounts().distinctUntilChanged()
        ) { total, monthly, avgRating, genreStrings, monthlyCounts ->
            BaseStats(total, monthly, avgRating, genreStrings, monthlyCounts)
        }

        val combinedFlow = combine(
            baseStatsFlow,
            preferencesRepository.getMonthlyWatchGoal().distinctUntilChanged(),
            userRatingRepository.getRatingDistribution().distinctUntilChanged(),
            watchHistoryRepository.getDailyWatchCounts(threeMonthsAgoMillis).distinctUntilChanged()
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
    }.flowOn(Dispatchers.Default)

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

    companion object {
        // 캘린더 히트맵 표시 범위와 일치하는 3개월(약 90일) 밀리초
        private const val THREE_MONTHS_MILLIS = 90L * 24 * 60 * 60 * 1000
    }
}
