package com.choo.moviefinder.presentation.stats

import app.cash.turbine.test
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.GenreCount
import com.choo.moviefinder.domain.model.MonthlyWatchCount
import com.choo.moviefinder.domain.model.WatchStats
import com.choo.moviefinder.domain.usecase.GetWatchStatsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getWatchStatsUseCase: GetWatchStatsUseCase

    private val testStats = WatchStats(
        totalWatched = 42,
        monthlyWatched = 5,
        averageRating = 3.7f,
        topGenres = listOf(
            GenreCount("Action", 10),
            GenreCount("Drama", 8),
            GenreCount("Comedy", 6)
        ),
        allGenreCounts = listOf(
            GenreCount("Action", 10),
            GenreCount("Drama", 8),
            GenreCount("Comedy", 6)
        ),
        monthlyWatchCounts = listOf(
            MonthlyWatchCount("2026-01", 3),
            MonthlyWatchCount("2026-02", 5)
        ),
        monthlyWatchGoal = 10
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getWatchStatsUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): StatsViewModel {
        return StatsViewModel(getWatchStatsUseCase)
    }

    @Test
    fun `initial state is Loading`() = runTest {
        every { getWatchStatsUseCase() } returns flowOf(testStats)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(StatsUiState.Loading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Success when usecase returns stats`() = runTest {
        every { getWatchStatsUseCase() } returns flowOf(testStats)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val first = awaitItem()
            if (first is StatsUiState.Loading) {
                val second = awaitItem()
                assertTrue(second is StatsUiState.Success)
                assertEquals(testStats, (second as StatsUiState.Success).stats)
            } else {
                assertTrue(first is StatsUiState.Success)
                assertEquals(testStats, (first as StatsUiState.Success).stats)
            }
        }
    }

    @Test
    fun `emits Success with empty genres`() = runTest {
        val statsWithEmptyGenres = testStats.copy(
            topGenres = emptyList(),
            allGenreCounts = emptyList()
        )
        every { getWatchStatsUseCase() } returns flowOf(statsWithEmptyGenres)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val first = awaitItem()
            val successState = if (first is StatsUiState.Loading) {
                awaitItem() as StatsUiState.Success
            } else {
                first as StatsUiState.Success
            }
            assertTrue(successState.stats.topGenres.isEmpty())
            assertTrue(successState.stats.allGenreCounts.isEmpty())
        }
    }

    @Test
    fun `emits Success with null average rating`() = runTest {
        val statsWithNullRating = testStats.copy(averageRating = null)
        every { getWatchStatsUseCase() } returns flowOf(statsWithNullRating)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val first = awaitItem()
            val successState = if (first is StatsUiState.Loading) {
                awaitItem() as StatsUiState.Success
            } else {
                first as StatsUiState.Success
            }
            assertNull(successState.stats.averageRating)
        }
    }

    @Test
    fun `emits Error on usecase IOException`() = runTest {
        every { getWatchStatsUseCase() } returns flow { throw IOException("Network error") }
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val first = awaitItem()
            val errorState = if (first is StatsUiState.Loading) {
                awaitItem() as StatsUiState.Error
            } else {
                first as StatsUiState.Error
            }
            assertEquals(ErrorType.NETWORK, errorState.errorType)
        }
    }

    @Test
    fun `emits Error on unknown exception`() = runTest {
        every { getWatchStatsUseCase() } returns flow { throw IllegalStateException("Unexpected error") }
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val first = awaitItem()
            val errorState = if (first is StatsUiState.Loading) {
                awaitItem() as StatsUiState.Error
            } else {
                first as StatsUiState.Error
            }
            assertEquals(ErrorType.UNKNOWN, errorState.errorType)
        }
    }

    @Test
    fun `Success state contains correct watch goal`() = runTest {
        val statsWithGoal = testStats.copy(monthlyWatchGoal = 20)
        every { getWatchStatsUseCase() } returns flowOf(statsWithGoal)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val first = awaitItem()
            val successState = if (first is StatsUiState.Loading) {
                awaitItem() as StatsUiState.Success
            } else {
                first as StatsUiState.Success
            }
            assertEquals(20, successState.stats.monthlyWatchGoal)
        }
    }
}
