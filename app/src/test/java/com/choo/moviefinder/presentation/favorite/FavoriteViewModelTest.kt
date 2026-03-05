package com.choo.moviefinder.presentation.favorite

import app.cash.turbine.test
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.usecase.GetFavoriteMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetWatchlistUseCase
import com.choo.moviefinder.domain.usecase.ToggleFavoriteUseCase
import com.choo.moviefinder.domain.usecase.ToggleWatchlistUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getFavoriteMoviesUseCase: GetFavoriteMoviesUseCase
    private lateinit var getWatchlistUseCase: GetWatchlistUseCase
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase
    private lateinit var toggleWatchlistUseCase: ToggleWatchlistUseCase

    private val testMovies = listOf(
        Movie(1, "Movie 1", "/poster1.jpg", "/backdrop1.jpg", "Overview 1", "2024-01-01", 8.0, 500),
        Movie(2, "Movie 2", "/poster2.jpg", "/backdrop2.jpg", "Overview 2", "2024-02-01", 7.5, 300)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getFavoriteMoviesUseCase = mockk()
        getWatchlistUseCase = mockk()
        toggleFavoriteUseCase = mockk()
        toggleWatchlistUseCase = mockk()

        every { getWatchlistUseCase() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): FavoriteViewModel {
        return FavoriteViewModel(
            getFavoriteMoviesUseCase,
            getWatchlistUseCase,
            toggleFavoriteUseCase,
            toggleWatchlistUseCase
        )
    }

    @Test
    fun `favoriteMovies emits movie list from use case`() = runTest {
        every { getFavoriteMoviesUseCase() } returns flowOf(testMovies)

        val viewModel = createViewModel()

        viewModel.favoriteMovies.test {
            val item = awaitItem()
            if (item.isEmpty()) {
                assertEquals(testMovies, awaitItem())
            } else {
                assertEquals(testMovies, item)
            }
        }
    }

    @Test
    fun `favoriteMovies emits empty list when no favorites`() = runTest {
        every { getFavoriteMoviesUseCase() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        viewModel.favoriteMovies.test {
            val item = awaitItem()
            assertTrue(item.isEmpty())
        }
    }

    @Test
    fun `toggleFavorite calls toggle use case`() = runTest {
        every { getFavoriteMoviesUseCase() } returns flowOf(testMovies)
        coEvery { toggleFavoriteUseCase(any()) } returns Unit

        val viewModel = createViewModel()

        viewModel.toggleFavorite(testMovies[0])
        advanceUntilIdle()

        coVerify { toggleFavoriteUseCase(testMovies[0]) }
    }

    @Test
    fun `toggleFavorite does not crash on error`() = runTest {
        every { getFavoriteMoviesUseCase() } returns flowOf(testMovies)
        coEvery { toggleFavoriteUseCase(any()) } throws RuntimeException("DB error")

        val viewModel = createViewModel()

        viewModel.toggleFavorite(testMovies[0])
        advanceUntilIdle()

        coVerify { toggleFavoriteUseCase(testMovies[0]) }
    }

    @Test
    fun `watchlistMovies emits movie list from use case`() = runTest {
        every { getFavoriteMoviesUseCase() } returns flowOf(emptyList())
        every { getWatchlistUseCase() } returns flowOf(testMovies)

        val viewModel = createViewModel()

        viewModel.watchlistMovies.test {
            val item = awaitItem()
            if (item.isEmpty()) {
                assertEquals(testMovies, awaitItem())
            } else {
                assertEquals(testMovies, item)
            }
        }
    }

    @Test
    fun `toggleWatchlist calls use case and handles error`() = runTest {
        every { getFavoriteMoviesUseCase() } returns flowOf(testMovies)
        coEvery { toggleWatchlistUseCase(any()) } returns Unit

        val viewModel = createViewModel()

        viewModel.toggleWatchlist(testMovies[0])
        advanceUntilIdle()

        coVerify { toggleWatchlistUseCase(testMovies[0]) }

        // Also verify error handling
        coEvery { toggleWatchlistUseCase(any()) } throws RuntimeException("DB error")
        viewModel.toggleWatchlist(testMovies[1])
        advanceUntilIdle()

        coVerify { toggleWatchlistUseCase(testMovies[1]) }
    }

    @Test
    fun `toggleFavorite error sends snackbar event`() = runTest {
        every { getFavoriteMoviesUseCase() } returns flowOf(testMovies)
        coEvery { toggleFavoriteUseCase(any()) } throws RuntimeException("DB error")

        val viewModel = createViewModel()

        viewModel.snackbarEvent.test {
            viewModel.toggleFavorite(testMovies[0])
            advanceUntilIdle()

            val errorType = awaitItem()
            assertEquals(ErrorType.UNKNOWN, errorType)
        }
    }

    @Test
    fun `onSortOrderSelected changes sort order`() = runTest {
        every { getFavoriteMoviesUseCase() } returns flowOf(testMovies)

        val viewModel = createViewModel()

        assertEquals(FavoriteSortOrder.ADDED_DATE, viewModel.sortOrder.value)

        viewModel.onSortOrderSelected(FavoriteSortOrder.TITLE)
        assertEquals(FavoriteSortOrder.TITLE, viewModel.sortOrder.value)
    }

    @Test
    fun `favoriteMovies sorted by title`() = runTest {
        val movies = listOf(
            Movie(1, "Zorro", "/p1.jpg", "/b1.jpg", "O1", "2024-01-01", 8.0, 500),
            Movie(2, "Avengers", "/p2.jpg", "/b2.jpg", "O2", "2024-02-01", 7.5, 300)
        )
        every { getFavoriteMoviesUseCase() } returns flowOf(movies)

        val viewModel = createViewModel()
        viewModel.onSortOrderSelected(FavoriteSortOrder.TITLE)

        viewModel.favoriteMovies.test {
            val item = awaitItem()
            if (item.isEmpty() || item == movies) {
                val sorted = awaitItem()
                assertEquals("Avengers", sorted[0].title)
                assertEquals("Zorro", sorted[1].title)
            } else {
                assertEquals("Avengers", item[0].title)
                assertEquals("Zorro", item[1].title)
            }
        }
    }

    @Test
    fun `favoriteMovies sorted by rating`() = runTest {
        val movies = listOf(
            Movie(1, "Movie A", "/p1.jpg", "/b1.jpg", "O1", "2024-01-01", 6.0, 500),
            Movie(2, "Movie B", "/p2.jpg", "/b2.jpg", "O2", "2024-02-01", 9.0, 300)
        )
        every { getFavoriteMoviesUseCase() } returns flowOf(movies)

        val viewModel = createViewModel()
        viewModel.onSortOrderSelected(FavoriteSortOrder.RATING)

        viewModel.favoriteMovies.test {
            val item = awaitItem()
            if (item.isEmpty() || item == movies) {
                val sorted = awaitItem()
                assertEquals("Movie B", sorted[0].title)
                assertEquals("Movie A", sorted[1].title)
            } else {
                assertEquals("Movie B", item[0].title)
                assertEquals("Movie A", item[1].title)
            }
        }
    }
}
