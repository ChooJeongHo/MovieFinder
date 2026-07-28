package com.choo.moviefinder.presentation.home

import androidx.lifecycle.SavedStateHandle
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
import com.choo.moviefinder.domain.usecase.GetWeeklyBoxOfficeWithTmdbMatchUseCase
import app.cash.turbine.test
import com.choo.moviefinder.util.CoroutineTestBase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
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
    private lateinit var getWeeklyBoxOfficeWithTmdbMatchUseCase: GetWeeklyBoxOfficeWithTmdbMatchUseCase

    private val testMovies = listOf(
        Movie(1, "Movie 1", "/p1.jpg", "/b1.jpg", "Overview 1", "2024-01-01", 8.0, 100),
        Movie(2, "Movie 2", "/p2.jpg", "/b2.jpg", "Overview 2", "2024-02-01", 7.5, 200)
    )

    private fun boxOfficeMovie(rank: Int, movieName: String) = BoxOfficeMovie(
        boxOffice = BoxOffice(rank, 0, false, "cd$rank", movieName, "2024-01-01", 1000L, 5000L, 10_000_000L, 100),
        matchedMovie = testMovies[0]
    )

    private val dailyItems = listOf(boxOfficeMovie(1, "일별 영화"))
    private val weeklyItems = listOf(boxOfficeMovie(1, "주간 영화"))

    @Before
    fun setup() {
        getNowPlayingMoviesUseCase = mockk()
        getPopularMoviesUseCase = mockk()
        getTrendingMoviesUseCase = mockk()
        getUpcomingMoviesUseCase = mockk()
        getWatchHistoryUseCase = mockk()
        getDailyBoxOfficeWithTmdbMatchUseCase = mockk()
        getWeeklyBoxOfficeWithTmdbMatchUseCase = mockk()

        every { getNowPlayingMoviesUseCase() } returns flowOf(PagingData.from(testMovies))
        every { getPopularMoviesUseCase() } returns flowOf(PagingData.from(testMovies))
        every { getTrendingMoviesUseCase() } returns flowOf(PagingData.from(testMovies))
        every { getUpcomingMoviesUseCase() } returns flowOf(PagingData.from(testMovies))
        every { getWatchHistoryUseCase() } returns flowOf(emptyList())
        // 일별/주간 두 경로 모두 명시적으로 스텁한다 — 한쪽만 스텁하면 잘못된 경로 호출이
        // MockK 예외가 아니라 조용한 통과로 숨겨질 수 있다
        coEvery { getDailyBoxOfficeWithTmdbMatchUseCase() } returns dailyItems
        coEvery { getWeeklyBoxOfficeWithTmdbMatchUseCase() } returns weeklyItems
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()): HomeViewModel {
        return HomeViewModel(
            getNowPlayingMoviesUseCase = getNowPlayingMoviesUseCase,
            getPopularMoviesUseCase = getPopularMoviesUseCase,
            getTrendingMoviesUseCase = getTrendingMoviesUseCase,
            getUpcomingMoviesUseCase = getUpcomingMoviesUseCase,
            getWatchHistoryUseCase = getWatchHistoryUseCase,
            getDailyBoxOfficeWithTmdbMatchUseCase = getDailyBoxOfficeWithTmdbMatchUseCase,
            getWeeklyBoxOfficeWithTmdbMatchUseCase = getWeeklyBoxOfficeWithTmdbMatchUseCase,
            savedStateHandle = savedStateHandle
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

    // ── 박스오피스 (일별/주간) ────────────────────────────────────

    @Test
    fun `boxOfficeUiState loads daily period on init and never calls weekly`() = runTest {
        val viewModel = createViewModel()

        advanceUntilIdle()

        val state = viewModel.boxOfficeUiState.value
        assertEquals(BoxOfficePeriod.DAILY, state.period)
        assertEquals(dailyItems, state.items)
        assertTrue(!state.isLoading)
        assertNull(state.errorType)
        coVerify(exactly = 1) { getDailyBoxOfficeWithTmdbMatchUseCase() }
        coVerify(exactly = 0) { getWeeklyBoxOfficeWithTmdbMatchUseCase() }
    }

    @Test
    fun `boxOfficeUiState is loading before the initial load completes`() = runTest {
        val viewModel = createViewModel()

        assertTrue(viewModel.boxOfficeUiState.value.isLoading)
    }

    @Test
    fun `onBoxOfficePeriodSelected WEEKLY loads weekly items`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onBoxOfficePeriodSelected(BoxOfficePeriod.WEEKLY)
        advanceUntilIdle()

        val state = viewModel.boxOfficeUiState.value
        assertEquals(BoxOfficePeriod.WEEKLY, state.period)
        assertEquals(weeklyItems, state.items)
        coVerify(exactly = 1) { getWeeklyBoxOfficeWithTmdbMatchUseCase() }
        coVerify(exactly = 1) { getDailyBoxOfficeWithTmdbMatchUseCase() }
    }

    // 동일 기간을 다시 선택하면 재로딩하지 않아야 한다 (칩 재선택/역동기화로 인한 중복 호출 방지)
    @Test
    fun `onBoxOfficePeriodSelected with same period does not reload`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onBoxOfficePeriodSelected(BoxOfficePeriod.DAILY)
        advanceUntilIdle()

        coVerify(exactly = 1) { getDailyBoxOfficeWithTmdbMatchUseCase() }
        coVerify(exactly = 0) { getWeeklyBoxOfficeWithTmdbMatchUseCase() }
    }

    @Test
    fun `boxOfficeUiState carries errorType when use case throws`() = runTest {
        coEvery { getDailyBoxOfficeWithTmdbMatchUseCase() } throws java.io.IOException("network down")
        val viewModel = createViewModel()

        advanceUntilIdle()

        val state = viewModel.boxOfficeUiState.value
        assertNotNull(state.errorType)
        assertTrue(!state.isLoading)
        assertTrue(state.items.isEmpty())
    }

    // 주간 실패 후에도 선택된 기간(WEEKLY)은 유지돼야 재시도가 올바른 경로를 탄다
    @Test
    fun `weekly failure keeps WEEKLY period in state`() = runTest {
        coEvery { getWeeklyBoxOfficeWithTmdbMatchUseCase() } throws java.io.IOException("network down")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onBoxOfficePeriodSelected(BoxOfficePeriod.WEEKLY)
        advanceUntilIdle()

        val state = viewModel.boxOfficeUiState.value
        assertEquals(BoxOfficePeriod.WEEKLY, state.period)
        assertNotNull(state.errorType)
    }

    @Test
    fun `retryBoxOffice re-invokes daily use case when daily period selected`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.retryBoxOffice()
        advanceUntilIdle()

        coVerify(exactly = 2) { getDailyBoxOfficeWithTmdbMatchUseCase() }
        coVerify(exactly = 0) { getWeeklyBoxOfficeWithTmdbMatchUseCase() }
    }

    // 회귀 탐지: retry가 기간을 하드코딩(daily)하면 이 테스트가 깨진다
    @Test
    fun `retryBoxOffice follows currently selected WEEKLY period`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onBoxOfficePeriodSelected(BoxOfficePeriod.WEEKLY)
        advanceUntilIdle()
        viewModel.retryBoxOffice()
        advanceUntilIdle()

        coVerify(exactly = 2) { getWeeklyBoxOfficeWithTmdbMatchUseCase() }
        coVerify(exactly = 1) { getDailyBoxOfficeWithTmdbMatchUseCase() }
        assertEquals(weeklyItems, viewModel.boxOfficeUiState.value.items)
    }

    // 빠른 연속 토글: 느린 weekly 응답이 나중에 도착해도 최신 선택(DAILY) 결과를 덮어쓰면 안 된다
    @Test
    fun `rapid period toggle cancels stale load and keeps latest period result`() = runTest {
        coEvery { getWeeklyBoxOfficeWithTmdbMatchUseCase() } coAnswers {
            delay(1000)
            weeklyItems
        }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onBoxOfficePeriodSelected(BoxOfficePeriod.WEEKLY)
        viewModel.onBoxOfficePeriodSelected(BoxOfficePeriod.DAILY)
        advanceUntilIdle()

        val state = viewModel.boxOfficeUiState.value
        assertEquals(BoxOfficePeriod.DAILY, state.period)
        assertEquals(dailyItems, state.items)
        assertTrue(!state.isLoading)
    }

    @Test
    fun `saved WEEKLY period is restored and drives the initial load`() = runTest {
        val viewModel = createViewModel(SavedStateHandle(mapOf("boxOfficePeriod" to "WEEKLY")))

        advanceUntilIdle()

        val state = viewModel.boxOfficeUiState.value
        assertEquals(BoxOfficePeriod.WEEKLY, state.period)
        assertEquals(weeklyItems, state.items)
        coVerify(exactly = 1) { getWeeklyBoxOfficeWithTmdbMatchUseCase() }
        coVerify(exactly = 0) { getDailyBoxOfficeWithTmdbMatchUseCase() }
    }

    // 알 수 없는 값이 복원되면 기본 DAILY로 폴백해야 한다 (enum valueOf 예외로 죽지 않도록)
    @Test
    fun `unknown saved period falls back to DAILY`() = runTest {
        val viewModel = createViewModel(SavedStateHandle(mapOf("boxOfficePeriod" to "MONTHLY")))

        advanceUntilIdle()

        assertEquals(BoxOfficePeriod.DAILY, viewModel.boxOfficeUiState.value.period)
        coVerify(exactly = 1) { getDailyBoxOfficeWithTmdbMatchUseCase() }
        coVerify(exactly = 0) { getWeeklyBoxOfficeWithTmdbMatchUseCase() }
    }

    @Test
    fun `onBoxOfficePeriodSelected persists selection to SavedStateHandle`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        viewModel.onBoxOfficePeriodSelected(BoxOfficePeriod.WEEKLY)
        advanceUntilIdle()

        assertEquals("WEEKLY", savedStateHandle.get<String>("boxOfficePeriod"))
    }
}
