package com.choo.moviefinder.data.repository

import com.choo.moviefinder.core.notification.ReleaseNotificationScheduler
import com.choo.moviefinder.data.local.dao.ScheduledReminderDao
import com.choo.moviefinder.data.local.entity.ScheduledReminderEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReminderRepositoryImplTest {

    private lateinit var scheduledReminderDao: ScheduledReminderDao
    private lateinit var releaseNotificationScheduler: ReleaseNotificationScheduler
    private lateinit var repository: ReminderRepositoryImpl

    private fun reminderEntity(movieId: Int) = ScheduledReminderEntity(
        movieId = movieId,
        movieTitle = "Movie $movieId",
        releaseDate = "2025-06-01",
        scheduledAt = 1_748_736_000_000L
    )

    @Before
    fun setUp() {
        scheduledReminderDao = mockk(relaxUnitFun = true)
        releaseNotificationScheduler = mockk(relaxUnitFun = true)
        repository = ReminderRepositoryImpl(scheduledReminderDao, releaseNotificationScheduler)
    }

    @Test
    fun `getAllReminders returns mapped reminders from dao`() = runTest {
        every { scheduledReminderDao.getAllReminders() } returns flowOf(
            listOf(reminderEntity(1), reminderEntity(2))
        )

        val result = repository.getAllReminders().first()

        assertEquals(2, result.size)
        assertEquals(1, result[0].movieId)
        assertEquals("Movie 1", result[0].movieTitle)
    }

    @Test
    fun `getAllReminders returns empty list when no reminders`() = runTest {
        every { scheduledReminderDao.getAllReminders() } returns flowOf(emptyList())

        val result = repository.getAllReminders().first()

        assertEquals(0, result.size)
    }

    @Test
    fun `scheduleReminder inserts entity into dao before scheduling WorkManager`() = runTest {
        val insertedSlot = slot<ScheduledReminderEntity>()
        coEvery { scheduledReminderDao.insertReminder(capture(insertedSlot)) } returns Unit

        repository.scheduleReminder(movieId = 5, movieTitle = "Inception", releaseDate = "2025-09-01")

        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            scheduledReminderDao.insertReminder(any())
            releaseNotificationScheduler.schedule(5, "Inception", "2025-09-01")
        }
        assertEquals(5, insertedSlot.captured.movieId)
        assertEquals("Inception", insertedSlot.captured.movieTitle)
        assertEquals("2025-09-01", insertedSlot.captured.releaseDate)
    }

    @Test
    fun `cancelReminder cancels WorkManager before deleting from dao`() = runTest {
        repository.cancelReminder(movieId = 3)

        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            releaseNotificationScheduler.cancel(3)
            scheduledReminderDao.deleteReminder(3)
        }
    }

    @Test
    fun `cancelReminder passes correct movieId to dao and scheduler`() = runTest {
        repository.cancelReminder(movieId = 99)

        coVerify(exactly = 1) { releaseNotificationScheduler.cancel(99) }
        coVerify(exactly = 1) { scheduledReminderDao.deleteReminder(99) }
    }
}
