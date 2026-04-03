package com.choo.moviefinder.domain.usecase

import app.cash.turbine.test
import com.choo.moviefinder.domain.model.GenreCount
import com.choo.moviefinder.domain.model.MonthlyWatchCount

import com.choo.moviefinder.domain.repository.PreferencesRepository
import com.choo.moviefinder.domain.repository.UserRatingRepository
import com.choo.moviefinder.domain.repository.WatchHistoryRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetWatchStatsUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var watchHistoryRepository: WatchHistoryRepository
    private lateinit var userRatingRepository: UserRatingRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var useCase: GetWatchStatsUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        watchHistoryRepository = mockk()
        userRatingRepository = mockk()
        preferencesRepository = mockk()

        every { watchHistoryRepository.getTotalWatchedCount() } returns flowOf(0)
        every { watchHistoryRepository.getWatchedCountSince(any()) } returns flowOf(0)
        every { userRatingRepository.getAverageUserRating() } returns flowOf(null)
        every { watchHistoryRepository.getGenreCounts() } returns flowOf(emptyList())
        every { watchHistoryRepository.getMonthlyWatchCounts() } returns flowOf(emptyList())
        every { userRatingRepository.getRatingDistribution() } returns flowOf(emptyList())
        every { watchHistoryRepository.getDailyWatchCounts(any()) } returns flowOf(emptyList())
        every { preferencesRepository.getMonthlyWatchGoal() } returns flowOf(0)

        useCase = GetWatchStatsUseCase(watchHistoryRepository, userRatingRepository, preferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `returns correct total and monthly watched counts`() = runTest {
        every { watchHistoryRepository.getTotalWatchedCount() } returns flowOf(42)
        every { watchHistoryRepository.getWatchedCountSince(any()) } returns flowOf(7)

        useCase().test {
            val stats = awaitItem()
            assertEquals(42, stats.totalWatched)
            assertEquals(7, stats.monthlyWatched)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `returns correct average rating`() = runTest {
        every { userRatingRepository.getAverageUserRating() } returns flowOf(4.5f)

        useCase().test {
            val stats = awaitItem()
            assertEquals(4.5f, stats.averageRating)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `returns null average rating when no ratings exist`() = runTest {
        every { userRatingRepository.getAverageUserRating() } returns flowOf(null)

        useCase().test {
            val stats = awaitItem()
            assertNull(stats.averageRating)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `returns genre counts sorted by frequency from normalized table`() = runTest {
        every { watchHistoryRepository.getGenreCounts() } returns flowOf(
            listOf(GenreCount("Action", 2), GenreCount("Drama", 2), GenreCount("Comedy", 1))
        )

        useCase().test {
            val stats = awaitItem()
            val allGenres = stats.allGenreCounts
            assertEquals(GenreCount("Action", 2), allGenres[0])
            assertEquals(GenreCount("Drama", 2), allGenres[1])
            assertEquals(GenreCount("Comedy", 1), allGenres[2])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `returns top 3 genres only in topGenres`() = runTest {
        every { watchHistoryRepository.getGenreCounts() } returns flowOf(
            listOf(
                GenreCount("Action", 5), GenreCount("Drama", 4), GenreCount("Comedy", 3),
                GenreCount("Thriller", 2), GenreCount("Horror", 1)
            )
        )

        useCase().test {
            val stats = awaitItem()
            assertEquals(3, stats.topGenres.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `handles empty genre strings`() = runTest {
        every { watchHistoryRepository.getGenreCounts() } returns flowOf(emptyList())

        useCase().test {
            val stats = awaitItem()
            assertEquals(emptyList<GenreCount>(), stats.topGenres)
            assertEquals(emptyList<GenreCount>(), stats.allGenreCounts)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `returns genre counts directly from repository`() = runTest {
        every { watchHistoryRepository.getGenreCounts() } returns flowOf(
            listOf(GenreCount("Action", 2), GenreCount("Drama", 1))
        )

        useCase().test {
            val stats = awaitItem()
            val actionGenre = stats.allGenreCounts.find { it.name == "Action" }
            val dramaGenre = stats.allGenreCounts.find { it.name == "Drama" }
            assertEquals(2, actionGenre?.count)
            assertEquals(1, dramaGenre?.count)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `returns monthly watch counts mapped correctly`() = runTest {
        val monthlyCounts = listOf(
            MonthlyWatchCount("2026-01", 5),
            MonthlyWatchCount("2026-02", 8),
            MonthlyWatchCount("2026-03", 3)
        )
        every { watchHistoryRepository.getMonthlyWatchCounts() } returns flowOf(monthlyCounts)

        useCase().test {
            val stats = awaitItem()
            val expected = listOf(
                MonthlyWatchCount("2026-01", 5),
                MonthlyWatchCount("2026-02", 8),
                MonthlyWatchCount("2026-03", 3)
            )
            assertEquals(expected, stats.monthlyWatchCounts)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `propagates monthly watch goal from preferences`() = runTest {
        every { preferencesRepository.getMonthlyWatchGoal() } returns flowOf(20)

        useCase().test {
            val stats = awaitItem()
            assertEquals(20, stats.monthlyWatchGoal)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `passes through genre counts from repository without modification`() = runTest {
        every { watchHistoryRepository.getGenreCounts() } returns flowOf(
            listOf(GenreCount("Action", 3), GenreCount("Drama", 1))
        )

        useCase().test {
            val stats = awaitItem()
            val actionGenre = stats.allGenreCounts.find { it.name == "Action" }
            val dramaGenre = stats.allGenreCounts.find { it.name == "Drama" }
            assertEquals(3, actionGenre?.count)
            assertEquals(1, dramaGenre?.count)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
