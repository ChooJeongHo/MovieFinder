package com.choo.moviefinder.core.notification

import android.content.Context
import com.choo.moviefinder.domain.usecase.CheckWatchGoalAchievedUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WatchGoalNotificationHelperTest {

    private lateinit var context: Context
    private lateinit var checkWatchGoalAchievedUseCase: CheckWatchGoalAchievedUseCase
    private lateinit var helper: WatchGoalNotificationHelper

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        checkWatchGoalAchievedUseCase = mockk()
        helper = spyk(
            WatchGoalNotificationHelper(context, checkWatchGoalAchievedUseCase),
            recordPrivateCalls = true
        )
        every { helper.showGoalAchievedNotification() } returns Unit
    }

    @Test
    fun `goal achieved - shows notification`() = runTest {
        coEvery { checkWatchGoalAchievedUseCase() } returns true

        helper.checkAndNotifyGoalAchieved()

        verify(exactly = 1) { helper.showGoalAchievedNotification() }
    }

    @Test
    fun `goal not achieved - does not show notification`() = runTest {
        coEvery { checkWatchGoalAchievedUseCase() } returns false

        helper.checkAndNotifyGoalAchieved()

        verify(exactly = 0) { helper.showGoalAchievedNotification() }
    }
}
