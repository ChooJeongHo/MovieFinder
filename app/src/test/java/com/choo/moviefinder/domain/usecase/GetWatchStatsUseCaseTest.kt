package com.choo.moviefinder.domain.usecase

import app.cash.turbine.test
import com.choo.moviefinder.domain.model.GenreCount
import com.choo.moviefinder.domain.model.MonthlyWatchCount

import com.choo.moviefinder.domain.repository.MovieRepository
import com.choo.moviefinder.domain.repository.PreferencesRepository
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

    private lateinit var movieRepository: MovieRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var useCase: GetWatchStatsUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        movieRepository = mockk()
        preferencesRepository = mockk()

        every { movieRepository.getTotalWatchedCount() } returns flowOf(0)
        every { movieRepository.getWatchedCountSince(any()) } returns flowOf(0)
        every { movieRepository.getAverageUserRating() } returns flowOf(null)
        every { movieRepository.getAllWatchedGenres() } returns flowOf(emptyList())
        every { movieRepository.getMonthlyWatchCounts() } returns flowOf(emptyList())
        every { preferencesRepository.getMonthlyWatchGoal() } returns flowOf(0)

        useCase = GetWatchStatsUseCase(movieRepository, preferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `returns correct total and monthly watched counts`() = runTest {
        every { movieRepository.getTotalWatchedCount() } returns flowOf(42)
        every { movieRepository.getWatchedCountSince(any()) } returns flowOf(7)

        useCase().test {
            val stats = awaitItem()
            assertEquals(42, stats.totalWatched)
            assertEquals(7, stats.monthlyWatched)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `returns correct average rating`() = runTest {
        every { movieRepository.getAverageUserRating() } returns flowOf(4.5f)

        useCase().test {
            val stats = awaitItem()
            assertEquals(4.5f, stats.averageRating)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `returns null average rating when no ratings exist`() = runTest {
        every { movieRepository.getAverageUserRating() } returns flowOf(null)

        useCase().test {
            val stats = awaitItem()
            assertNull(stats.averageRating)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `computes genre counts sorted by frequency`() = runTest {
        every { movieRepository.getAllWatchedGenres() } returns flowOf(
            listOf("Action,Drama", "Action,Comedy", "Drama")
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
        every { movieRepository.getAllWatchedGenres() } returns flowOf(
            listOf("Action,Drama,Comedy,Thriller,Horror")
        )

        useCase().test {
            val stats = awaitItem()
            assertEquals(3, stats.topGenres.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `handles empty genre strings`() = runTest {
        every { movieRepository.getAllWatchedGenres() } returns flowOf(emptyList())

        useCase().test {
            val stats = awaitItem()
            assertEquals(emptyList<GenreCount>(), stats.topGenres)
            assertEquals(emptyList<GenreCount>(), stats.allGenreCounts)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `handles comma-separated genre strings correctly`() = runTest {
        every { movieRepository.getAllWatchedGenres() } returns flowOf(
            listOf("Action,Drama", "Action")
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
        every { movieRepository.getMonthlyWatchCounts() } returns flowOf(monthlyCounts)

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
    fun `handles genres with whitespace trimming`() = runTest {
        every { movieRepository.getAllWatchedGenres() } returns flowOf(
            listOf(" Action , Drama ", "Action")
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
}
