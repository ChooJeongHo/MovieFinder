package com.choo.moviefinder.presentation.home

import androidx.paging.PagingData
import com.choo.moviefinder.domain.model.BoxOffice
import com.choo.moviefinder.domain.model.BoxOfficeMovie
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.usecase.GetDailyBoxOfficeWithTmdbMatchUseCase
import com.choo.moviefinder.domain.usecase.GetNowPlayingMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetPopularMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetTrendingMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetUpcomingMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetWatchHistoryUseCase
import app.cash.turbine.test
import com.choo.moviefinder.util.CoroutineTestBase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest : CoroutineTestBase() {

    private lateinit var getNowPlayingMoviesUseCase: GetNowPlayingMoviesUseCase
    private lateinit var getPopularMoviesUseCase: GetPopularMoviesUseCase
    private lateinit var getTrendingMoviesUseCase: GetTrendingMoviesUseCase
    private lateinit var getUpcomingMoviesUseCase: GetUpcomingMoviesUseCase
    private lateinit var getWatchHistoryUseCase: GetWatchHistoryUseCase
    private lateinit var getDailyBoxOfficeWithTmdbMatchUseCase: GetDailyBoxOfficeWithTmdbMatchUseCase

    private val testMovies = listOf(
        Movie(1, "Movie 1", "/p1.jpg", "/b1.jpg", "Overview 1", "2024-01-01", 8.0, 100),
        Movie(2, "Movie 2", "/p2.jpg", "/b2.jpg", "Overview 2", "2024-02-01", 7.5, 200)
    )

    @Before
    fun setup() {
        getNowPlayingMoviesUseCase = mockk()
        getPopularMoviesUseCase = mockk()
        getTrendingMoviesUseCase = mockk()
        getUpcomingMoviesUseCase = mockk()
        getWatchHistoryUseCase = mockk()
        getDailyBoxOfficeWithTmdbMatchUseCase = mockk()

        every { getNowPlayingMoviesUseCase() } returns flowOf(PagingData.from(testMovies))
        every { getPopularMoviesUseCase() } returns flowOf(PagingData.from(testMovies))
        every { getTrendingMoviesUseCase() } returns flowOf(PagingData.from(testMovies))
        every { getUpcomingMoviesUseCase() } returns flowOf(PagingData.from(testMovies))
        every { getWatchHistoryUseCase() } returns flowOf(emptyList())
        coEvery { getDailyBoxOfficeWithTmdbMatchUseCase() } returns emptyList()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            getNowPlayingMoviesUseCase = getNowPlayingMoviesUseCase,
            getPopularMoviesUseCase = getPopularMoviesUseCase,
            getTrendingMoviesUseCase = getTrendingMoviesUseCase,
            getUpcomingMoviesUseCase = getUpcomingMoviesUseCase,
            getWatchHistoryUseCase = getWatchHistoryUseCase,
            getDailyBoxOfficeWithTmdbMatchUseCase = getDailyBoxOfficeWithTmdbMatchUseCase
        )
    }

    @Test
    fun `paging use cases are not invoked on construction`() {
        createViewModel()
        verify(exactly = 0) { getNowPlayingMoviesUseCase() }
        verify(exactly = 0) { getPopularMoviesUseCase() }
        verify(exactly = 0) { getTrendingMoviesUseCase() }
        verify(exactly = 0) { getUpcomingMoviesUseCase() }
    }

    @Test
    fun `currentMovies uses nowPlaying source for default tab`() = runTest {
        val viewModel = createViewModel()
        viewModel.currentMovies.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 1) { getNowPlayingMoviesUseCase() }
        verify(exactly = 0) { getPopularMoviesUseCase() }
        verify(exactly = 0) { getTrendingMoviesUseCase() }
        verify(exactly = 0) { getUpcomingMoviesUseCase() }
    }

    @Test
    fun `currentMovies switches to correct source on tab change`() = runTest {
        val viewModel = createViewModel()
        viewModel.currentMovies.test {
            awaitItem()
            viewModel.onTabSelected(HomeTab.POPULAR)
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 1) { getPopularMoviesUseCase() }
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
    fun `watchHistory limits results to 20 items`() = runTest {
        val manyMovies = (1..30).map {
            Movie(it, "Movie $it", "/p.jpg", "/b.jpg", "Overview", "2024-01-01", 7.0, 100)
        }
        every { getWatchHistoryUseCase() } returns flowOf(manyMovies)
        val viewModel = createViewModel()

        viewModel.watchHistory.test {
            val item = awaitItem()
            if (item.isEmpty()) {
                assertEquals(20, awaitItem().size)
            } else {
                assertEquals(20, item.size)
            }
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

    @Test
    fun `boxOfficeUiState emits Success with items from use case on init`() = runTest {
        val boxOfficeMovies = listOf(
            BoxOfficeMovie(
                boxOffice = BoxOffice(1, 0, false, "20240001", "영화 1", "2024-01-01", 1000L, 5000L, 10_000_000L, 100),
                matchedMovie = testMovies[0]
            )
        )
        coEvery { getDailyBoxOfficeWithTmdbMatchUseCase() } returns boxOfficeMovies
        val viewModel = createViewModel()

        advanceUntilIdle()

        assertEquals(BoxOfficeUiState.Success(boxOfficeMovies), viewModel.boxOfficeUiState.value)
    }

    @Test
    fun `boxOfficeUiState emits Error when use case throws`() = runTest {
        coEvery { getDailyBoxOfficeWithTmdbMatchUseCase() } throws java.io.IOException("network down")
        val viewModel = createViewModel()

        advanceUntilIdle()

        assertTrue(viewModel.boxOfficeUiState.value is BoxOfficeUiState.Error)
    }

    @Test
    fun `retryBoxOffice re-invokes use case`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.retryBoxOffice()
        advanceUntilIdle()

        coVerify(exactly = 2) { getDailyBoxOfficeWithTmdbMatchUseCase() }
    }
}
