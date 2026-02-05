package com.choo.moviefinder.presentation.home

import androidx.paging.PagingData
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.usecase.GetNowPlayingMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetPopularMoviesUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getNowPlayingMoviesUseCase: GetNowPlayingMoviesUseCase
    private lateinit var getPopularMoviesUseCase: GetPopularMoviesUseCase

    private val testMovies = listOf(
        Movie(1, "Movie 1", "/p1.jpg", "/b1.jpg", "Overview 1", "2024-01-01", 8.0, 100),
        Movie(2, "Movie 2", "/p2.jpg", "/b2.jpg", "Overview 2", "2024-02-01", 7.5, 200)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getNowPlayingMoviesUseCase = mockk()
        getPopularMoviesUseCase = mockk()

        every { getNowPlayingMoviesUseCase() } returns flowOf(PagingData.from(testMovies))
        every { getPopularMoviesUseCase() } returns flowOf(PagingData.from(testMovies))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            getNowPlayingMoviesUseCase = getNowPlayingMoviesUseCase,
            getPopularMoviesUseCase = getPopularMoviesUseCase
        )
    }

    @Test
    fun `nowPlayingMovies is initialized and invokes use case`() {
        val viewModel = createViewModel()

        assertNotNull(viewModel.nowPlayingMovies)
        verify(exactly = 1) { getNowPlayingMoviesUseCase() }
    }

    @Test
    fun `popularMovies is initialized and invokes use case`() {
        val viewModel = createViewModel()

        assertNotNull(viewModel.popularMovies)
        verify(exactly = 1) { getPopularMoviesUseCase() }
    }

    @Test
    fun `both use cases are invoked on construction`() {
        createViewModel()

        verify(exactly = 1) { getNowPlayingMoviesUseCase() }
        verify(exactly = 1) { getPopularMoviesUseCase() }
    }
}
