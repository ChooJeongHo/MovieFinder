@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.core.util.currentYearMonth
import com.choo.moviefinder.domain.repository.PreferencesRepository
import com.choo.moviefinder.domain.repository.WatchHistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckWatchGoalAchievedUseCaseTest {

    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var watchHistoryRepository: WatchHistoryRepository
    private lateinit var useCase: CheckWatchGoalAchievedUseCase

    @Before
    fun setup() {
        preferencesRepository = mockk(relaxed = true)
        watchHistoryRepository = mockk(relaxed = true)
        useCase = CheckWatchGoalAchievedUseCase(preferencesRepository, watchHistoryRepository)
    }

    @Test
    fun `goal is 0 - returns false without checking watch count`() = runTest {
        every { preferencesRepository.getMonthlyWatchGoal() } returns flowOf(0)

        val result = useCase()

        assertFalse(result)
        coVerify(exactly = 0) { watchHistoryRepository.getWatchedCountSince(any()) }
    }

    @Test
    fun `negative goal - returns false`() = runTest {
        every { preferencesRepository.getMonthlyWatchGoal() } returns flowOf(-1)

        val result = useCase()

        assertFalse(result)
    }

    @Test
    fun `goal not yet reached - returns false`() = runTest {
        every { preferencesRepository.getMonthlyWatchGoal() } returns flowOf(10)
        every { watchHistoryRepository.getWatchedCountSince(any()) } returns flowOf(5)

        val result = useCase()

        assertFalse(result)
        coVerify(exactly = 0) { preferencesRepository.setLastGoalNotifiedMonth(any()) }
    }

    @Test
    fun `goal reached and not yet notified - saves month and returns true`() = runTest {
        every { preferencesRepository.getMonthlyWatchGoal() } returns flowOf(5)
        every { watchHistoryRepository.getWatchedCountSince(any()) } returns flowOf(5)
        every { preferencesRepository.getLastGoalNotifiedMonth() } returns flowOf("")
        coEvery { preferencesRepository.setLastGoalNotifiedMonth(any()) } returns Unit

        val result = useCase()

        assertTrue(result)
        coVerify(exactly = 1) {
            preferencesRepository.setLastGoalNotifiedMonth(match { it.matches(Regex("\\d{4}-\\d{2}")) })
        }
    }

    @Test
    fun `goal reached but already notified this month - returns false`() = runTest {
        every { preferencesRepository.getMonthlyWatchGoal() } returns flowOf(5)
        every { watchHistoryRepository.getWatchedCountSince(any()) } returns flowOf(10)
        every { preferencesRepository.getLastGoalNotifiedMonth() } returns flowOf(currentYearMonth())

        val result = useCase()

        assertFalse(result)
        coVerify(exactly = 0) { preferencesRepository.setLastGoalNotifiedMonth(any()) }
    }

    @Test
    fun `goal exceeded and not yet notified - returns true`() = runTest {
        every { preferencesRepository.getMonthlyWatchGoal() } returns flowOf(3)
        every { watchHistoryRepository.getWatchedCountSince(any()) } returns flowOf(10)
        every { preferencesRepository.getLastGoalNotifiedMonth() } returns flowOf("")
        coEvery { preferencesRepository.setLastGoalNotifiedMonth(any()) } returns Unit

        val result = useCase()

        assertTrue(result)
    }

    @Test
    fun `notified last month but goal reached this month - returns true`() = runTest {
        every { preferencesRepository.getMonthlyWatchGoal() } returns flowOf(5)
        every { watchHistoryRepository.getWatchedCountSince(any()) } returns flowOf(5)
        every { preferencesRepository.getLastGoalNotifiedMonth() } returns flowOf("2025-01")
        coEvery { preferencesRepository.setLastGoalNotifiedMonth(any()) } returns Unit

        val result = useCase()

        assertTrue(result)
        coVerify(exactly = 1) {
            preferencesRepository.setLastGoalNotifiedMonth(match { it != "2025-01" })
        }
    }
}
