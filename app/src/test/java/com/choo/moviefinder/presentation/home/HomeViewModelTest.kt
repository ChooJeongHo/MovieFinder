package com.choo.moviefinder.presentation.home

import androidx.paging.PagingData
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.usecase.GetNowPlayingMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetPopularMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetTrendingMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetWatchHistoryUseCase
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getNowPlayingMoviesUseCase: GetNowPlayingMoviesUseCase
    private lateinit var getPopularMoviesUseCase: GetPopularMoviesUseCase
    private lateinit var getTrendingMoviesUseCase: GetTrendingMoviesUseCase
    private lateinit var getWatchHistoryUseCase: GetWatchHistoryUseCase

    private val testMovies = listOf(
        Movie(1, "Movie 1", "/p1.jpg", "/b1.jpg", "Overview 1", "2024-01-01", 8.0, 100),
        Movie(2, "Movie 2", "/p2.jpg", "/b2.jpg", "Overview 2", "2024-02-01", 7.5, 200)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getNowPlayingMoviesUseCase = mockk()
        getPopularMoviesUseCase = mockk()
        getTrendingMoviesUseCase = mockk()
        getWatchHistoryUseCase = mockk()

        every { getNowPlayingMoviesUseCase() } returns flowOf(PagingData.from(testMovies))
        every { getPopularMoviesUseCase() } returns flowOf(PagingData.from(testMovies))
        every { getTrendingMoviesUseCase() } returns flowOf(PagingData.from(testMovies))
        every { getWatchHistoryUseCase() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            getNowPlayingMoviesUseCase = getNowPlayingMoviesUseCase,
            getPopularMoviesUseCase = getPopularMoviesUseCase,
            getTrendingMoviesUseCase = getTrendingMoviesUseCase,
            getWatchHistoryUseCase = getWatchHistoryUseCase
        )
    }

    @Test
    fun `nowPlayingMovies is lazily initialized on first access`() {
        val viewModel = createViewModel()

        verify(exactly = 0) { getNowPlayingMoviesUseCase() }
        assertNotNull(viewModel.nowPlayingMovies)
        verify(exactly = 1) { getNowPlayingMoviesUseCase() }
    }

    @Test
    fun `popularMovies is lazily initialized on first access`() {
        val viewModel = createViewModel()

        verify(exactly = 0) { getPopularMoviesUseCase() }
        assertNotNull(viewModel.popularMovies)
        verify(exactly = 1) { getPopularMoviesUseCase() }
    }

    @Test
    fun `trendingMovies is lazily initialized on first access`() {
        val viewModel = createViewModel()

        verify(exactly = 0) { getTrendingMoviesUseCase() }
        assertNotNull(viewModel.trendingMovies)
        verify(exactly = 1) { getTrendingMoviesUseCase() }
    }

    @Test
    fun `paging use cases are not invoked on construction`() {
        createViewModel()

        verify(exactly = 0) { getNowPlayingMoviesUseCase() }
        verify(exactly = 0) { getPopularMoviesUseCase() }
        verify(exactly = 0) { getTrendingMoviesUseCase() }
    }

    @Test
    fun `watchHistory emits movie list from use case`() = runTest {
        every { getWatchHistoryUseCase() } returns flowOf(testMovies)
        val viewModel = createViewModel()

        viewModel.watchHistory.test {
            val item = awaitItem()
            if (item.isEmpty()) {
                assertEquals(testMovies, awaitItem())
            } else {
                assertEquals(testMovies, item)
            }
        }
    }

    @Test
    fun `watchHistory emits empty list by default`() = runTest {
        val viewModel = createViewModel()

        viewModel.watchHistory.test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun `selectedTab defaults to NOW_PLAYING`() {
        val viewModel = createViewModel()

        assertEquals(HomeTab.NOW_PLAYING, viewModel.selectedTab.value)
    }

    @Test
    fun `onTabSelected updates selectedTab`() {
        val viewModel = createViewModel()

        viewModel.onTabSelected(HomeTab.POPULAR)
        assertEquals(HomeTab.POPULAR, viewModel.selectedTab.value)

        viewModel.onTabSelected(HomeTab.TRENDING)
        assertEquals(HomeTab.TRENDING, viewModel.selectedTab.value)
    }
}
