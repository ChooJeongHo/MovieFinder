package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.WatchlistDao
import com.choo.moviefinder.data.local.entity.WatchlistEntity
import com.choo.moviefinder.domain.model.FavoriteSortOrder
import com.choo.moviefinder.domain.model.Movie
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WatchlistRepositoryImplTest {

    private lateinit var watchlistDao: WatchlistDao
    private lateinit var repository: WatchlistRepositoryImpl

    private fun entity(id: Int) = WatchlistEntity(
        id = id, title = "Movie $id", posterPath = null, backdropPath = null,
        overview = "", releaseDate = "2024-01-01", voteAverage = 7.0, voteCount = 100
    )

    private fun movie(id: Int) = Movie(
        id = id, title = "Movie $id", posterPath = null, backdropPath = null,
        overview = "", releaseDate = "2024-01-01", voteAverage = 7.0, voteCount = 100
    )

    @Before
    fun setUp() {
        watchlistDao = mockk(relaxUnitFun = true)
        repository = WatchlistRepositoryImpl(watchlistDao)
    }

    @Test
    fun `getWatchlistMovies returns mapped movies from dao`() = runTest {
        every { watchlistDao.getAllWatchlist() } returns flowOf(listOf(entity(1), entity(2)))

        val result = repository.getWatchlistMovies().first()

        assertEquals(2, result.size)
        assertEquals(1, result[0].id)
        assertEquals(2, result[1].id)
    }

    @Test
    fun `getWatchlistMovies returns empty list when dao is empty`() = runTest {
        every { watchlistDao.getAllWatchlist() } returns flowOf(emptyList())

        val result = repository.getWatchlistMovies().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getWatchlistMoviesSorted ADDED_DATE delegates to getAllWatchlist`() = runTest {
        every { watchlistDao.getAllWatchlist() } returns flowOf(listOf(entity(1)))

        repository.getWatchlistMoviesSorted(FavoriteSortOrder.ADDED_DATE).first()

        verify(exactly = 1) { watchlistDao.getAllWatchlist() }
        verify(exactly = 0) { watchlistDao.getAllWatchlistSortedByTitle() }
        verify(exactly = 0) { watchlistDao.getAllWatchlistSortedByRating() }
    }

    @Test
    fun `getWatchlistMoviesSorted TITLE delegates to getAllWatchlistSortedByTitle`() = runTest {
        every { watchlistDao.getAllWatchlistSortedByTitle() } returns flowOf(listOf(entity(1)))

        repository.getWatchlistMoviesSorted(FavoriteSortOrder.TITLE).first()

        verify(exactly = 1) { watchlistDao.getAllWatchlistSortedByTitle() }
        verify(exactly = 0) { watchlistDao.getAllWatchlist() }
    }

    @Test
    fun `getWatchlistMoviesSorted RATING delegates to getAllWatchlistSortedByRating`() = runTest {
        every { watchlistDao.getAllWatchlistSortedByRating() } returns flowOf(listOf(entity(1)))

        repository.getWatchlistMoviesSorted(FavoriteSortOrder.RATING).first()

        verify(exactly = 1) { watchlistDao.getAllWatchlistSortedByRating() }
        verify(exactly = 0) { watchlistDao.getAllWatchlist() }
    }

    @Test
    fun `toggleWatchlist calls dao toggleWatchlist`() = runTest {
        repository.toggleWatchlist(movie(1))

        coVerify(exactly = 1) { watchlistDao.toggleWatchlist(any()) }
    }

    @Test
    fun `isInWatchlist delegates to dao`() = runTest {
        every { watchlistDao.isInWatchlist(42) } returns flowOf(true)

        val result = repository.isInWatchlist(42).first()

        assertEquals(true, result)
        verify(exactly = 1) { watchlistDao.isInWatchlist(42) }
    }

    @Test
    fun `searchWatchlistMovies returns mapped movies`() = runTest {
        coEvery { watchlistDao.searchWatchlist("dune") } returns listOf(entity(5))

        val result = repository.searchWatchlistMovies("dune")

        assertEquals(1, result.size)
        assertEquals(5, result[0].id)
    }

    @Test
    fun `setReminder delegates to dao`() = runTest {
        repository.setReminder(movieId = 7, dateMillis = 1_700_000_000_000L)

        coVerify(exactly = 1) { watchlistDao.setReminder(7, 1_700_000_000_000L) }
    }

    @Test
    fun `clearReminder delegates to dao`() = runTest {
        repository.clearReminder(movieId = 7)

        coVerify(exactly = 1) { watchlistDao.clearReminder(7) }
    }

    @Test
    fun `getMoviesWithReminder maps entities to WatchlistReminder`() = runTest {
        val e = entity(10).copy(reminderDate = 1_700_000_000_000L)
        coEvery { watchlistDao.getMoviesWithReminder() } returns listOf(e)

        val result = repository.getMoviesWithReminder()

        assertEquals(1, result.size)
        assertEquals(10, result[0].movieId)
        assertEquals("Movie 10", result[0].title)
        assertEquals(1_700_000_000_000L, result[0].reminderDate)
    }

    @Test
    fun `observeMoviesWithReminder maps entities to WatchlistReminder flow`() = runTest {
        val e = entity(11).copy(reminderDate = null)
        every { watchlistDao.observeMoviesWithReminder() } returns flowOf(listOf(e))

        val result = repository.observeMoviesWithReminder().first()

        assertEquals(1, result.size)
        assertEquals(11, result[0].movieId)
        assertEquals(null, result[0].reminderDate)
    }
}
