package com.choo.moviefinder.presentation.settings

import app.cash.turbine.test
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.ThemeMode
import com.choo.moviefinder.domain.usecase.ClearWatchHistoryUseCase
import com.choo.moviefinder.domain.usecase.ExportUserDataUseCase
import com.choo.moviefinder.domain.usecase.GetMonthlyWatchGoalUseCase
import com.choo.moviefinder.domain.usecase.GetThemeModeUseCase
import com.choo.moviefinder.domain.usecase.ImportUserDataUseCase
import com.choo.moviefinder.domain.usecase.SetMonthlyWatchGoalUseCase
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
    private lateinit var getMonthlyWatchGoalUseCase: GetMonthlyWatchGoalUseCase
    private lateinit var setMonthlyWatchGoalUseCase: SetMonthlyWatchGoalUseCase
    private lateinit var exportUserDataUseCase: ExportUserDataUseCase
    private lateinit var importUserDataUseCase: ImportUserDataUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getThemeModeUseCase = mockk()
        setThemeModeUseCase = mockk()
        clearWatchHistoryUseCase = mockk()
        getMonthlyWatchGoalUseCase = mockk()
        setMonthlyWatchGoalUseCase = mockk()
        exportUserDataUseCase = mockk()
        importUserDataUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        themeFlow: kotlinx.coroutines.flow.Flow<ThemeMode> = flowOf(ThemeMode.SYSTEM)
    ): SettingsViewModel {
        every { getThemeModeUseCase() } returns themeFlow
        every { getMonthlyWatchGoalUseCase() } returns flowOf(0)
        return SettingsViewModel(
            getThemeModeUseCase,
            setThemeModeUseCase,
            clearWatchHistoryUseCase,
            getMonthlyWatchGoalUseCase,
            setMonthlyWatchGoalUseCase,
            exportUserDataUseCase,
            importUserDataUseCase
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

    @Test
    fun `clearWatchHistory success emits watchHistoryCleared event`() = runTest {
        coEvery { clearWatchHistoryUseCase() } returns Unit
        val viewModel = createViewModel()

        viewModel.watchHistoryCleared.test {
            viewModel.clearWatchHistory()
            advanceUntilIdle()
            awaitItem()
        }
    }

    @Test
    fun `clearWatchHistory error emits snackbar event`() = runTest {
        coEvery { clearWatchHistoryUseCase() } throws RuntimeException("DB error")
        val viewModel = createViewModel()

        viewModel.snackbarEvent.test {
            viewModel.clearWatchHistory()
            advanceUntilIdle()
            assertEquals(ErrorType.UNKNOWN, awaitItem())
        }
    }

    private fun createViewModelWithGoal(
        goalFlow: kotlinx.coroutines.flow.Flow<Int> = flowOf(0)
    ): SettingsViewModel {
        every { getThemeModeUseCase() } returns flowOf(ThemeMode.SYSTEM)
        every { getMonthlyWatchGoalUseCase() } returns goalFlow
        return SettingsViewModel(
            getThemeModeUseCase,
            setThemeModeUseCase,
            clearWatchHistoryUseCase,
            getMonthlyWatchGoalUseCase,
            setMonthlyWatchGoalUseCase,
            exportUserDataUseCase,
            importUserDataUseCase
        )
    }

    @Test
    fun `monthlyWatchGoal returns 0 by default`() = runTest {
        val viewModel = createViewModelWithGoal()

        viewModel.monthlyWatchGoal.test {
            assertEquals(0, awaitItem())
        }
    }

    @Test
    fun `monthlyWatchGoal reflects value from use case`() = runTest {
        val viewModel = createViewModelWithGoal(flowOf(10))

        viewModel.monthlyWatchGoal.test {
            val item = awaitItem()
            if (item == 0) {
                assertEquals(10, awaitItem())
            } else {
                assertEquals(10, item)
            }
        }
    }

    @Test
    fun `setMonthlyWatchGoal calls use case with correct value`() = runTest {
        coEvery { setMonthlyWatchGoalUseCase(any()) } returns Unit
        val viewModel = createViewModelWithGoal()

        viewModel.setMonthlyWatchGoal(15)
        advanceUntilIdle()

        coVerify { setMonthlyWatchGoalUseCase(15) }
    }

    @Test
    fun `setMonthlyWatchGoal does not crash on error`() = runTest {
        coEvery { setMonthlyWatchGoalUseCase(any()) } throws RuntimeException("DataStore error")
        val viewModel = createViewModelWithGoal()

        viewModel.setMonthlyWatchGoal(10)
        advanceUntilIdle()

        coVerify { setMonthlyWatchGoalUseCase(10) }
    }

    @Test
    fun `exportData emits json string on success`() = runTest {
        val backup = com.choo.moviefinder.domain.model.UserDataBackup()
        coEvery { exportUserDataUseCase() } returns backup
        val viewModel = createViewModel()

        viewModel.exportedJson.test {
            viewModel.exportData()
            advanceUntilIdle()
            val json = awaitItem()
            assert(json.isNotBlank())
        }
    }

    @Test
    fun `exportData sends snackbar on error`() = runTest {
        coEvery { exportUserDataUseCase() } throws RuntimeException("Export error")
        val viewModel = createViewModel()

        viewModel.snackbarEvent.test {
            viewModel.exportData()
            advanceUntilIdle()
            assertEquals(com.choo.moviefinder.core.util.ErrorType.UNKNOWN, awaitItem())
        }
    }

    @Test
    fun `importData sends success event`() = runTest {
        coEvery { importUserDataUseCase(any()) } returns Unit
        val viewModel = createViewModel()
        val validJson = """{"version":1,"exportedAt":0,"favorites":[],"watchlist":[],"ratings":[],"memos":[]}"""

        viewModel.importSuccess.test {
            viewModel.importData(validJson)
            advanceUntilIdle()
            awaitItem()
        }
    }

    @Test
    fun `importData sends snackbar on error`() = runTest {
        val viewModel = createViewModel()
        val invalidJson = "not valid json"

        viewModel.snackbarEvent.test {
            viewModel.importData(invalidJson)
            advanceUntilIdle()
            val errorType = awaitItem()
            assert(errorType != null)
        }
    }
}
