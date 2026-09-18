package com.choo.moviefinder.core.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

// 사용자가 날짜 피커로 알림 시각을 다시 고르면 그 즉시 반영돼야 하므로 REPLACE가 맞다
// (ReleaseNotificationScheduler의 KEEP과 대비되는 사례 — 110일차 WorkManager 정책 전수 조사 참고).
@OptIn(ExperimentalCoroutinesApi::class)
class WatchlistReminderSchedulerTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WatchlistReminderScheduler

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        workManager = mockk(relaxed = true)

        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any()) } returns workManager

        every { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) } returns mockk(relaxed = true)
        every { workManager.cancelUniqueWork(any()) } returns mockk(relaxed = true)

        scheduler = WatchlistReminderScheduler(context)
    }

    @After
    fun tearDown() {
        unmockkObject(WorkManager.Companion)
    }

    @Test
    fun `schedule enqueues work for future reminder time`() = runTest {
        scheduler.schedule(
            movieId = 1,
            movieTitle = "Test Movie",
            dateMillis = System.currentTimeMillis() + 60_000L
        )

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
        }
    }

    @Test
    fun `schedule skips past reminder time`() = runTest {
        scheduler.schedule(
            movieId = 1,
            movieTitle = "Old Movie",
            dateMillis = System.currentTimeMillis() - 60_000L
        )

        verify(exactly = 0) {
            workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
        }
    }

    @Test
    fun `schedule uses correct work name`() = runTest {
        val movieId = 99
        val workNameSlot = slot<String>()
        every {
            workManager.enqueueUniqueWork(capture(workNameSlot), any(), any<OneTimeWorkRequest>())
        } returns mockk(relaxed = true)

        scheduler.schedule(
            movieId = movieId,
            movieTitle = "Named Movie",
            dateMillis = System.currentTimeMillis() + 60_000L
        )

        assert(workNameSlot.captured == "watchlist_reminder_$movieId") {
            "Expected work name 'watchlist_reminder_$movieId' but was '${workNameSlot.captured}'"
        }
    }

    @Test
    fun `schedule uses ExistingWorkPolicy REPLACE so re-picking a date takes effect immediately`() = runTest {
        val policySlot = slot<ExistingWorkPolicy>()
        every {
            workManager.enqueueUniqueWork(any(), capture(policySlot), any<OneTimeWorkRequest>())
        } returns mockk(relaxed = true)

        scheduler.schedule(
            movieId = 7,
            movieTitle = "Policy Movie",
            dateMillis = System.currentTimeMillis() + 60_000L
        )

        assert(policySlot.captured == ExistingWorkPolicy.REPLACE) {
            "Expected ExistingWorkPolicy.REPLACE but was ${policySlot.captured}"
        }
    }

    @Test
    fun `cancel cancels unique work with correct name`() = runTest {
        val movieId = 42
        scheduler.cancel(movieId)

        verify(exactly = 1) { workManager.cancelUniqueWork("watchlist_reminder_$movieId") }
    }
}
