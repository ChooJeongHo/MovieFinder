package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.ScheduledReminder
import com.choo.moviefinder.domain.model.WatchlistReminder
import com.choo.moviefinder.domain.repository.ReminderRepository
import com.choo.moviefinder.domain.repository.WatchlistRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReminderUseCasesTest {

    private lateinit var reminderRepository: ReminderRepository
    private lateinit var watchlistRepository: WatchlistRepository

    private val testReminders = listOf(
        ScheduledReminder(
            movieId = 1,
            movieTitle = "Inception",
            releaseDate = "2010-07-16",
            scheduledAt = 1_278_288_000_000L
        ),
        ScheduledReminder(
            movieId = 2,
            movieTitle = "Tenet",
            releaseDate = "2020-09-03",
            scheduledAt = 1_599_091_200_000L
        )
    )

    private val testWatchlistReminders = listOf(
        WatchlistReminder(movieId = 10, title = "Dune", reminderDate = 1_700_000_000_000L),
        WatchlistReminder(movieId = 11, title = "Dune: Part Two", reminderDate = null)
    )

    @Before
    fun setUp() {
        reminderRepository = mockk(relaxUnitFun = true)
        watchlistRepository = mockk(relaxUnitFun = true)
    }

    // --- GetScheduledRemindersUseCase ---

    @Test
    fun `GetScheduledRemindersUseCase delegates to ReminderRepository getAllReminders`() {
        val flow = flowOf(testReminders)
        every { reminderRepository.getAllReminders() } returns flow
        val useCase = GetScheduledRemindersUseCase(reminderRepository)

        val result = useCase()

        verify(exactly = 1) { reminderRepository.getAllReminders() }
        assertEquals(flow, result)
    }

    @Test
    fun `GetScheduledRemindersUseCase returns empty list flow when repository has no reminders`() {
        every { reminderRepository.getAllReminders() } returns flowOf(emptyList())
        val useCase = GetScheduledRemindersUseCase(reminderRepository)

        useCase()

        verify(exactly = 1) { reminderRepository.getAllReminders() }
    }

    // --- CancelReminderUseCase ---

    @Test
    fun `CancelReminderUseCase delegates to ReminderRepository cancelReminder`() = runTest {
        val useCase = CancelReminderUseCase(reminderRepository)

        useCase(movieId = 1)

        coVerify(exactly = 1) { reminderRepository.cancelReminder(1) }
    }

    @Test
    fun `CancelReminderUseCase passes correct movieId to repository`() = runTest {
        val useCase = CancelReminderUseCase(reminderRepository)

        useCase(movieId = 99)

        coVerify(exactly = 1) { reminderRepository.cancelReminder(99) }
        coVerify(exactly = 0) { reminderRepository.cancelReminder(1) }
    }

    // --- SetWatchlistReminderUseCase ---

    @Test
    fun `SetWatchlistReminderUseCase delegates to WatchlistRepository setReminder`() = runTest {
        val useCase = SetWatchlistReminderUseCase(watchlistRepository)

        useCase(movieId = 5, dateMillis = 1_700_000_000_000L)

        coVerify(exactly = 1) { watchlistRepository.setReminder(5, 1_700_000_000_000L) }
    }

    @Test
    fun `SetWatchlistReminderUseCase passes correct movieId and dateMillis to repository`() = runTest {
        val useCase = SetWatchlistReminderUseCase(watchlistRepository)

        useCase(movieId = 42, dateMillis = 9_999_999_999_999L)

        coVerify(exactly = 1) { watchlistRepository.setReminder(42, 9_999_999_999_999L) }
    }

    // --- ClearWatchlistReminderUseCase ---

    @Test
    fun `ClearWatchlistReminderUseCase delegates to WatchlistRepository clearReminder`() = runTest {
        val useCase = ClearWatchlistReminderUseCase(watchlistRepository)

        useCase(movieId = 7)

        coVerify(exactly = 1) { watchlistRepository.clearReminder(7) }
    }

    @Test
    fun `ClearWatchlistReminderUseCase passes correct movieId to repository`() = runTest {
        val useCase = ClearWatchlistReminderUseCase(watchlistRepository)

        useCase(movieId = 123)

        coVerify(exactly = 1) { watchlistRepository.clearReminder(123) }
        coVerify(exactly = 0) { watchlistRepository.clearReminder(7) }
    }

    // --- GetWatchlistRemindersUseCase ---

    @Test
    fun `GetWatchlistRemindersUseCase delegates to WatchlistRepository getMoviesWithReminder`() = runTest {
        coEvery { watchlistRepository.getMoviesWithReminder() } returns testWatchlistReminders
        val useCase = GetWatchlistRemindersUseCase(watchlistRepository)

        val result = useCase()

        coVerify(exactly = 1) { watchlistRepository.getMoviesWithReminder() }
        assertEquals(testWatchlistReminders, result)
    }

    @Test
    fun `GetWatchlistRemindersUseCase returns empty list when no reminders set`() = runTest {
        coEvery { watchlistRepository.getMoviesWithReminder() } returns emptyList()
        val useCase = GetWatchlistRemindersUseCase(watchlistRepository)

        val result = useCase()

        assertEquals(emptyList<WatchlistReminder>(), result)
    }

    @Test
    fun `GetWatchlistRemindersUseCase returns list including entries with null reminderDate`() = runTest {
        val remindersWithNull = listOf(
            WatchlistReminder(movieId = 20, title = "No Date Movie", reminderDate = null)
        )
        coEvery { watchlistRepository.getMoviesWithReminder() } returns remindersWithNull
        val useCase = GetWatchlistRemindersUseCase(watchlistRepository)

        val result = useCase()

        assertEquals(1, result.size)
        assertEquals(null, result[0].reminderDate)
    }
}
