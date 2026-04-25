package com.choo.moviefinder.presentation.reminder

import app.cash.turbine.test
import com.choo.moviefinder.domain.model.ScheduledReminder
import com.choo.moviefinder.domain.usecase.CancelReminderUseCase
import com.choo.moviefinder.domain.usecase.GetScheduledRemindersUseCase
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
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getScheduledRemindersUseCase: GetScheduledRemindersUseCase
    private lateinit var cancelReminderUseCase: CancelReminderUseCase

    private val testReminders = listOf(
        ScheduledReminder(
            movieId = 1,
            movieTitle = "Dune: Part Two",
            releaseDate = "2024-03-01",
            scheduledAt = 1_700_000_000_000L
        ),
        ScheduledReminder(
            movieId = 2,
            movieTitle = "Alien: Romulus",
            releaseDate = "2024-08-16",
            scheduledAt = 1_710_000_000_000L
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getScheduledRemindersUseCase = mockk()
        cancelReminderUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ReminderHistoryViewModel {
        return ReminderHistoryViewModel(
            getScheduledRemindersUseCase = getScheduledRemindersUseCase,
            cancelReminderUseCase = cancelReminderUseCase
        )
    }

    @Test
    fun `reminders StateFlow emits list from getScheduledRemindersUseCase`() = runTest {
        every { getScheduledRemindersUseCase() } returns flowOf(testReminders)
        val viewModel = createViewModel()

        viewModel.reminders.test {
            // stateIn emits emptyList() as its initial value before the upstream flow is collected
            val first = awaitItem()
            val emitted = if (first.isEmpty()) awaitItem() else first
            assertEquals(testReminders, emitted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reminders StateFlow emits empty list when repository returns empty`() = runTest {
        every { getScheduledRemindersUseCase() } returns flowOf(emptyList())
        val viewModel = createViewModel()

        viewModel.reminders.test {
            assertEquals(emptyList<ScheduledReminder>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cancelReminder success sends cancelledEvent`() = runTest {
        every { getScheduledRemindersUseCase() } returns flowOf(testReminders)
        coEvery { cancelReminderUseCase(1) } returns Unit
        val viewModel = createViewModel()

        viewModel.cancelledEvent.test {
            viewModel.cancelReminder(1)
            advanceUntilIdle()
            assertEquals(Unit, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cancelReminder success does not send errorEvent`() = runTest {
        every { getScheduledRemindersUseCase() } returns flowOf(testReminders)
        coEvery { cancelReminderUseCase(1) } returns Unit
        val viewModel = createViewModel()

        viewModel.errorEvent.test {
            viewModel.cancelReminder(1)
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun `cancelReminder error sends errorEvent`() = runTest {
        every { getScheduledRemindersUseCase() } returns flowOf(testReminders)
        coEvery { cancelReminderUseCase(2) } throws IOException("DB error")
        val viewModel = createViewModel()

        viewModel.errorEvent.test {
            viewModel.cancelReminder(2)
            advanceUntilIdle()
            assertEquals(Unit, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cancelReminder error does not send cancelledEvent`() = runTest {
        every { getScheduledRemindersUseCase() } returns flowOf(testReminders)
        coEvery { cancelReminderUseCase(2) } throws RuntimeException("unexpected")
        val viewModel = createViewModel()

        viewModel.cancelledEvent.test {
            viewModel.cancelReminder(2)
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun `cancelReminder delegates to CancelReminderUseCase with correct movieId`() = runTest {
        every { getScheduledRemindersUseCase() } returns flowOf(testReminders)
        coEvery { cancelReminderUseCase(any()) } returns Unit
        val viewModel = createViewModel()

        viewModel.cancelReminder(42)
        advanceUntilIdle()

        coVerify(exactly = 1) { cancelReminderUseCase(42) }
    }
}
