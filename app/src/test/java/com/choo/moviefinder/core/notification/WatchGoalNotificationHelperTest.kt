@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.choo.moviefinder.core.notification

import android.content.Context
import com.choo.moviefinder.core.util.currentYearMonth
import com.choo.moviefinder.domain.repository.WatchHistoryRepository
import com.choo.moviefinder.domain.repository.PreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WatchGoalNotificationHelperTest {

    private lateinit var context: Context
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var watchHistoryRepository: WatchHistoryRepository
    private lateinit var helper: WatchGoalNotificationHelper

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)
        watchHistoryRepository = mockk(relaxed = true)
        helper = spyk(
            WatchGoalNotificationHelper(context, preferencesRepository, watchHistoryRepository),
            recordPrivateCalls = true
        )
        // Android 알림 API는 유닛 테스트에서 사용 불가하므로 스킵
        every { helper.showGoalAchievedNotification() } returns Unit
    }

    @Test
    fun `goal is 0 - early return without checking watch count`() = runTest {
        every { preferencesRepository.getMonthlyWatchGoal() } returns flowOf(0)

        helper.checkAndNotifyGoalAchieved()

        coVerify(exactly = 0) { watchHistoryRepository.getWatchedCountSince(any()) }
        verify(exactly = 0) { helper.showGoalAchievedNotification() }
    }

    @Test
    fun `negative goal - early return without checking watch count`() = runTest {
        every { preferencesRepository.getMonthlyWatchGoal() } returns flowOf(-1)

        helper.checkAndNotifyGoalAchieved()

        coVerify(exactly = 0) { watchHistoryRepository.getWatchedCountSince(any()) }
        verify(exactly = 0) { helper.showGoalAchievedNotification() }
    }

    @Test
    fun `goal not yet reached - does not notify`() = runTest {
        every { preferencesRepository.getMonthlyWatchGoal() } returns flowOf(10)
        every { watchHistoryRepository.getWatchedCountSince(any()) } returns flowOf(5)

        helper.checkAndNotifyGoalAchieved()

        coVerify(exactly = 0) { preferencesRepository.setLastGoalNotifiedMonth(any()) }
        verify(exactly = 0) { helper.showGoalAchievedNotification() }
    }

    @Test
    fun `goal reached and not yet notified - saves month and shows notification`() = runTest {
        every { preferencesRepository.getMonthlyWatchGoal() } returns flowOf(5)
        every { watchHistoryRepository.getWatchedCountSince(any()) } returns flowOf(5)
        every { preferencesRepository.getLastGoalNotifiedMonth() } returns flowOf("")
        coEvery { preferencesRepository.setLastGoalNotifiedMonth(any()) } returns Unit

        helper.checkAndNotifyGoalAchieved()

        coVerify(exactly = 1) {
            preferencesRepository.setLastGoalNotifiedMonth(match { it.matches(Regex("\\d{4}-\\d{2}")) })
        }
        verify(exactly = 1) { helper.showGoalAchievedNotification() }
    }

    @Test
    fun `goal reached but already notified this month - does not notify again`() = runTest {
        every { preferencesRepository.getMonthlyWatchGoal() } returns flowOf(5)
        every { watchHistoryRepository.getWatchedCountSince(any()) } returns flowOf(10)
        every { preferencesRepository.getLastGoalNotifiedMonth() } returns flowOf(currentYearMonth())

        helper.checkAndNotifyGoalAchieved()

        coVerify(exactly = 0) { preferencesRepository.setLastGoalNotifiedMonth(any()) }
        verify(exactly = 0) { helper.showGoalAchievedNotification() }
    }

    @Test
    fun `goal exceeded - still triggers notification if not yet notified`() = runTest {
        every { preferencesRepository.getMonthlyWatchGoal() } returns flowOf(3)
        every { watchHistoryRepository.getWatchedCountSince(any()) } returns flowOf(10)
        every { preferencesRepository.getLastGoalNotifiedMonth() } returns flowOf("")
        coEvery { preferencesRepository.setLastGoalNotifiedMonth(any()) } returns Unit

        helper.checkAndNotifyGoalAchieved()

        coVerify(exactly = 1) { preferencesRepository.setLastGoalNotifiedMonth(any()) }
        verify(exactly = 1) { helper.showGoalAchievedNotification() }
    }

    @Test
    fun `notified last month but goal reached this month - notifies again`() = runTest {
        every { preferencesRepository.getMonthlyWatchGoal() } returns flowOf(5)
        every { watchHistoryRepository.getWatchedCountSince(any()) } returns flowOf(5)
        every { preferencesRepository.getLastGoalNotifiedMonth() } returns flowOf("2025-01")
        coEvery { preferencesRepository.setLastGoalNotifiedMonth(any()) } returns Unit

        helper.checkAndNotifyGoalAchieved()

        coVerify(exactly = 1) {
            preferencesRepository.setLastGoalNotifiedMonth(match { it != "2025-01" })
        }
        verify(exactly = 1) { helper.showGoalAchievedNotification() }
    }
}
