package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.DailyCount
import com.choo.moviefinder.data.local.dao.GenreCountResult
import com.choo.moviefinder.data.local.dao.MonthlyCount
import com.choo.moviefinder.data.local.dao.WatchHistoryDao
import com.choo.moviefinder.domain.model.Movie
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WatchHistoryRepositoryImplTest {

    private lateinit var watchHistoryDao: WatchHistoryDao
    private lateinit var repository: WatchHistoryRepositoryImpl

    private val testMovie = Movie(
        id = 1, title = "Test", posterPath = "/p.jpg", backdropPath = "/b.jpg",
        overview = "Overview", releaseDate = "2024-01-01", voteAverage = 8.0, voteCount = 100
    )

    @Before
    fun setup() {
        watchHistoryDao = mockk(relaxUnitFun = true)
        repository = WatchHistoryRepositoryImpl(watchHistoryDao)
    }

    // ── saveWatchHistoryWithGenres ──────────────────────────────

    @Test
    fun `saveWatchHistoryWithGenres inserts movie entity`() = runTest {
        repository.saveWatchHistoryWithGenres(testMovie, "Action,Drama")

        coVerify { watchHistoryDao.insertWithGenres(match { it.movieId == 1 && it.title == "Test" }, any()) }
    }

    @Test
    fun `saveWatchHistoryWithGenres inserts genre entities for multiple genres`() = runTest {
        repository.saveWatchHistoryWithGenres(testMovie, "Action,Drama")

        coVerify {
            watchHistoryDao.insertWithGenres(
                any(),
                match { genres ->
                    genres.size == 2 &&
                        genres.any { it.genreName == "Action" } &&
                        genres.any { it.genreName == "Drama" }
                }
            )
        }
    }

    @Test
    fun `saveWatchHistoryWithGenres trims whitespace from genre names`() = runTest {
        repository.saveWatchHistoryWithGenres(testMovie, " Action , Drama ")

        coVerify {
            watchHistoryDao.insertWithGenres(
                any(),
                match { genres ->
                    genres.size == 2 &&
                        genres.all { it.genreName == it.genreName.trim() } &&
                        genres.any { it.genreName == "Action" } &&
                        genres.any { it.genreName == "Drama" }
                }
            )
        }
    }

    @Test
    fun `saveWatchHistoryWithGenres passes empty list when genres string is empty`() = runTest {
        repository.saveWatchHistoryWithGenres(testMovie, "")

        coVerify { watchHistoryDao.insertWithGenres(any(), emptyList()) }
    }

    @Test
    fun `saveWatchHistoryWithGenres filters blank entries from comma-only string`() = runTest {
        repository.saveWatchHistoryWithGenres(testMovie, ",,,")

        coVerify { watchHistoryDao.insertWithGenres(any(), emptyList()) }
    }

    @Test
    fun `saveWatchHistoryWithGenres passes placeholder watchHistoryId — DAO overwrites with real rowId`() = runTest {
        repository.saveWatchHistoryWithGenres(testMovie, "Action")

        coVerify {
            watchHistoryDao.insertWithGenres(
                any(),
                match { genres -> genres.all { it.watchHistoryId == 0L } }
            )
        }
    }

    // ── clearWatchHistory ──────────────────────────────────────

    @Test
    fun `clearWatchHistory delegates to clearAllWithGenres atomically`() = runTest {
        repository.clearWatchHistory()

        coVerify { watchHistoryDao.clearAllWithGenres() }
    }

    // ── getWatchHistory ───────────────────────────────────────

    @Test
    fun `getWatchHistory delegates to dao getRecentHistory`() = runTest {
        every { watchHistoryDao.getRecentHistory(any()) } returns flowOf(emptyList())

        repository.getWatchHistory().first()

        every { watchHistoryDao.getRecentHistory(any()) } returns flowOf(emptyList())
    }

    // ── saveWatchHistory ──────────────────────────────────────

    @Test
    fun `saveWatchHistory inserts entity via dao`() = runTest {
        coEvery { watchHistoryDao.insert(any()) } returns 1L

        repository.saveWatchHistory(testMovie)

        coVerify(exactly = 1) { watchHistoryDao.insert(any()) }
    }

    // ── getTotalWatchedCount ──────────────────────────────────

    @Test
    fun `getTotalWatchedCount returns count from dao`() = runTest {
        every { watchHistoryDao.getTotalCount() } returns flowOf(42)

        val result = repository.getTotalWatchedCount().first()

        assertEquals(42, result)
    }

    // ── getWatchedCountSince ──────────────────────────────────

    @Test
    fun `getWatchedCountSince delegates to dao with given timestamp`() = runTest {
        every { watchHistoryDao.getCountSince(1_000L) } returns flowOf(5)

        val result = repository.getWatchedCountSince(1_000L).first()

        assertEquals(5, result)
    }

    // ── getGenreCounts ────────────────────────────────────────

    @Test
    fun `getGenreCounts maps GenreCountResult to GenreCount domain model`() = runTest {
        every { watchHistoryDao.getGenreCounts() } returns flowOf(
            listOf(GenreCountResult("Action", 10), GenreCountResult("Drama", 5))
        )

        val result = repository.getGenreCounts().first()

        assertEquals(2, result.size)
        assertEquals("Action", result[0].name)
        assertEquals(10, result[0].count)
    }

    // ── getMonthlyWatchCounts ─────────────────────────────────

    @Test
    fun `getMonthlyWatchCounts maps MonthlyCount to MonthlyWatchCount domain model`() = runTest {
        every { watchHistoryDao.getMonthlyWatchCounts() } returns flowOf(
            listOf(MonthlyCount("2026-05", 8))
        )

        val result = repository.getMonthlyWatchCounts().first()

        assertEquals(1, result.size)
        assertEquals("2026-05", result[0].yearMonth)
        assertEquals(8, result[0].count)
    }

    // ── getDailyWatchCounts ───────────────────────────────────

    @Test
    fun `getDailyWatchCounts maps DailyCount to DailyWatchCount domain model`() = runTest {
        every { watchHistoryDao.getDailyWatchCounts(1_000L) } returns flowOf(
            listOf(DailyCount("2026-05-01", 3))
        )

        val result = repository.getDailyWatchCounts(1_000L).first()

        assertEquals(1, result.size)
        assertEquals("2026-05-01", result[0].date)
        assertEquals(3, result[0].count)
    }
}
