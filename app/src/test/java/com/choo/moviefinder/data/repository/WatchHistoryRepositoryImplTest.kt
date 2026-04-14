package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.WatchHistoryDao
import com.choo.moviefinder.domain.model.Movie
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
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

        coVerify { watchHistoryDao.insertWithGenres(match { it.id == 1 && it.title == "Test" }, any()) }
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
    fun `saveWatchHistoryWithGenres sets correct watchHistoryId on genre entities`() = runTest {
        repository.saveWatchHistoryWithGenres(testMovie, "Action")

        coVerify {
            watchHistoryDao.insertWithGenres(
                any(),
                match { genres -> genres.all { it.watchHistoryId == testMovie.id } }
            )
        }
    }

    // ── clearWatchHistory ──────────────────────────────────────

    @Test
    fun `clearWatchHistory delegates to clearAllWithGenres atomically`() = runTest {
        repository.clearWatchHistory()

        coVerify { watchHistoryDao.clearAllWithGenres() }
    }
}
