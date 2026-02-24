package com.choo.moviefinder.presentation.favorite

import app.cash.turbine.test
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.usecase.GetFavoriteMoviesUseCase
import com.choo.moviefinder.domain.usecase.ToggleFavoriteUseCase
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
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase

    private val testMovies = listOf(
        Movie(1, "Movie 1", "/poster1.jpg", "/backdrop1.jpg", "Overview 1", "2024-01-01", 8.0, 500),
        Movie(2, "Movie 2", "/poster2.jpg", "/backdrop2.jpg", "Overview 2", "2024-02-01", 7.5, 300)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getFavoriteMoviesUseCase = mockk()
        toggleFavoriteUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `favoriteMovies emits movie list from use case`() = runTest {
        every { getFavoriteMoviesUseCase() } returns flowOf(testMovies)

        val viewModel = FavoriteViewModel(getFavoriteMoviesUseCase, toggleFavoriteUseCase)

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

        val viewModel = FavoriteViewModel(getFavoriteMoviesUseCase, toggleFavoriteUseCase)

        viewModel.favoriteMovies.test {
            val item = awaitItem()
            assertTrue(item.isEmpty())
        }
    }

    @Test
    fun `toggleFavorite calls toggle use case`() = runTest {
        every { getFavoriteMoviesUseCase() } returns flowOf(testMovies)
        coEvery { toggleFavoriteUseCase(any()) } returns Unit

        val viewModel = FavoriteViewModel(getFavoriteMoviesUseCase, toggleFavoriteUseCase)

        viewModel.toggleFavorite(testMovies[0])
        advanceUntilIdle()

        coVerify { toggleFavoriteUseCase(testMovies[0]) }
    }

    @Test
    fun `toggleFavorite does not crash on error`() = runTest {
        every { getFavoriteMoviesUseCase() } returns flowOf(testMovies)
        coEvery { toggleFavoriteUseCase(any()) } throws RuntimeException("DB error")

        val viewModel = FavoriteViewModel(getFavoriteMoviesUseCase, toggleFavoriteUseCase)

        viewModel.toggleFavorite(testMovies[0])
        advanceUntilIdle()

        coVerify { toggleFavoriteUseCase(testMovies[0]) }
    }
}
