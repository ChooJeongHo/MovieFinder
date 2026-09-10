package com.choo.moviefinder.core.notification

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.choo.moviefinder.MainActivity
import com.choo.moviefinder.domain.usecase.CheckWatchGoalAchievedUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
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

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
        unmockkConstructor(Intent::class)
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

    // moviefinder://stats는 path 세그먼트가 없어 매니페스트 intent-filter(android:path="/")와
    // 어긋난다 (project_deeplink_onboarding_backstack_fix 메모 참고). implicit ACTION_VIEW만으로는
    // 실기기에서 PackageManager가 액티비티를 못 찾으므로, shortcuts.xml의 정적 단축키와 동일하게
    // MainActivity를 explicit component로 지정하는지 검증한다 — android.content.Intent의 getter는
    // JVM 유닛 테스트에서 항상 기본값을 반환하는 스텁이라 되읽을 수 없으므로(AppShortcutManagerTest와
    // 동일 사유) setClass 호출 자체를 mockkConstructor로 가로채 확인한다.
    @Test
    fun `goal achieved notification - deep links via explicit MainActivity component`() {
        mockkStatic(Uri::class)
        val testUri = mockk<Uri>(relaxed = true)
        every { Uri.parse("moviefinder://stats") } returns testUri

        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().setClass(any(), any()) } returns mockk(relaxed = true)
        every { anyConstructed<Intent>().setFlags(any()) } returns mockk(relaxed = true)

        val realHelper = WatchGoalNotificationHelper(context, checkWatchGoalAchievedUseCase)
        realHelper.buildStatsDeepLinkIntent()

        verify(exactly = 1) { Uri.parse("moviefinder://stats") }
        verify(exactly = 1) { anyConstructed<Intent>().setClass(context, MainActivity::class.java) }
    }
}
