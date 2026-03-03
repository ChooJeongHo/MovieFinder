package com.choo.moviefinder.presentation.settings

import app.cash.turbine.test
import com.choo.moviefinder.domain.model.ThemeMode
import com.choo.moviefinder.domain.usecase.ClearWatchHistoryUseCase
import com.choo.moviefinder.domain.usecase.GetThemeModeUseCase
import com.choo.moviefinder.domain.usecase.SetThemeModeUseCase
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getThemeModeUseCase: GetThemeModeUseCase
    private lateinit var setThemeModeUseCase: SetThemeModeUseCase
    private lateinit var clearWatchHistoryUseCase: ClearWatchHistoryUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getThemeModeUseCase = mockk()
        setThemeModeUseCase = mockk()
        clearWatchHistoryUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        themeFlow: kotlinx.coroutines.flow.Flow<ThemeMode> = flowOf(ThemeMode.SYSTEM)
    ): SettingsViewModel {
        every { getThemeModeUseCase() } returns themeFlow
        return SettingsViewModel(
            getThemeModeUseCase, setThemeModeUseCase, clearWatchHistoryUseCase
        )
    }

    @Test
    fun `currentThemeMode returns SYSTEM by default`() = runTest {
        val viewModel = createViewModel()

        viewModel.currentThemeMode.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem())
        }
    }

    @Test
    fun `currentThemeMode reflects DARK from use case`() = runTest {
        val viewModel = createViewModel(flowOf(ThemeMode.DARK))

        viewModel.currentThemeMode.test {
            val item = awaitItem()
            if (item == ThemeMode.SYSTEM) {
                assertEquals(ThemeMode.DARK, awaitItem())
            } else {
                assertEquals(ThemeMode.DARK, item)
            }
        }
    }

    @Test
    fun `currentThemeMode reflects LIGHT from use case`() = runTest {
        val viewModel = createViewModel(flowOf(ThemeMode.LIGHT))

        viewModel.currentThemeMode.test {
            val item = awaitItem()
            if (item == ThemeMode.SYSTEM) {
                assertEquals(ThemeMode.LIGHT, awaitItem())
            } else {
                assertEquals(ThemeMode.LIGHT, item)
            }
        }
    }

    @Test
    fun `setThemeMode calls use case with correct mode`() = runTest {
        coEvery { setThemeModeUseCase(any()) } returns Unit
        val viewModel = createViewModel()

        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        coVerify { setThemeModeUseCase(ThemeMode.DARK) }
    }

    @Test
    fun `setThemeMode with LIGHT calls use case`() = runTest {
        coEvery { setThemeModeUseCase(any()) } returns Unit
        val viewModel = createViewModel()

        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        coVerify { setThemeModeUseCase(ThemeMode.LIGHT) }
    }

    @Test
    fun `setThemeMode does not crash on error`() = runTest {
        coEvery { setThemeModeUseCase(any()) } throws RuntimeException("DataStore error")
        val viewModel = createViewModel()

        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        coVerify { setThemeModeUseCase(ThemeMode.DARK) }
    }

    @Test
    fun `clearWatchHistory calls use case`() = runTest {
        coEvery { clearWatchHistoryUseCase() } returns Unit
        val viewModel = createViewModel()

        viewModel.clearWatchHistory()
        advanceUntilIdle()

        coVerify { clearWatchHistoryUseCase() }
    }

    @Test
    fun `clearWatchHistory does not crash on error`() = runTest {
        coEvery { clearWatchHistoryUseCase() } throws RuntimeException("DB error")
        val viewModel = createViewModel()

        viewModel.clearWatchHistory()
        advanceUntilIdle()

        coVerify { clearWatchHistoryUseCase() }
    }
}
