package com.choo.moviefinder.presentation.widget

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

// WorkManager 정책이 뒤바뀌면 조용한 버그가 된다: periodic에 REPLACE를 쓰면 재등록마다 주기가
// 리셋돼 영영 실행되지 않고, one-time 재시도에 KEEP을 쓰면 백오프 대기 중엔 버튼을 눌러도
// 무반응처럼 보인다(110일차 실기기 검증으로 발견). 두 함수의 정책을 회귀 방지용으로 고정한다.
class BoxOfficeWidgetWorkerSchedulingTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        workManager = mockk(relaxed = true)

        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any()) } returns workManager

        every {
            workManager.enqueueUniquePeriodicWork(any(), any(), any<PeriodicWorkRequest>())
        } returns mockk(relaxed = true)
        every {
            workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
        } returns mockk(relaxed = true)
        every { workManager.cancelUniqueWork(any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkObject(WorkManager.Companion)
    }

    @Test
    fun `enqueuePeriodic uses KEEP so re-registration does not reset the recurring interval`() {
        val policySlot = slot<ExistingPeriodicWorkPolicy>()
        every {
            workManager.enqueueUniquePeriodicWork(any(), capture(policySlot), any<PeriodicWorkRequest>())
        } returns mockk(relaxed = true)

        BoxOfficeWidgetWorker.enqueuePeriodic(context)

        assert(policySlot.captured == ExistingPeriodicWorkPolicy.KEEP) {
            "Expected ExistingPeriodicWorkPolicy.KEEP but was ${policySlot.captured}"
        }
    }

    @Test
    fun `enqueuePeriodic uses the periodic work name`() {
        val nameSlot = slot<String>()
        every {
            workManager.enqueueUniquePeriodicWork(capture(nameSlot), any(), any<PeriodicWorkRequest>())
        } returns mockk(relaxed = true)

        BoxOfficeWidgetWorker.enqueuePeriodic(context)

        assert(nameSlot.captured == BoxOfficeWidgetWorker.PERIODIC_WORK_NAME)
    }

    @Test
    fun `enqueueOneTimeRefresh uses REPLACE so a refresh stuck in backoff retries immediately`() {
        val policySlot = slot<ExistingWorkPolicy>()
        every {
            workManager.enqueueUniqueWork(any(), capture(policySlot), any<OneTimeWorkRequest>())
        } returns mockk(relaxed = true)

        BoxOfficeWidgetWorker.enqueueOneTimeRefresh(context)

        assert(policySlot.captured == ExistingWorkPolicy.REPLACE) {
            "Expected ExistingWorkPolicy.REPLACE but was ${policySlot.captured}"
        }
    }

    @Test
    fun `enqueueOneTimeRefresh uses the one-time work name`() {
        val nameSlot = slot<String>()
        every {
            workManager.enqueueUniqueWork(capture(nameSlot), any(), any<OneTimeWorkRequest>())
        } returns mockk(relaxed = true)

        BoxOfficeWidgetWorker.enqueueOneTimeRefresh(context)

        assert(nameSlot.captured == BoxOfficeWidgetWorker.ONE_TIME_WORK_NAME)
    }

    @Test
    fun `cancelAll cancels both unique work names`() {
        BoxOfficeWidgetWorker.cancelAll(context)

        verify(exactly = 1) { workManager.cancelUniqueWork(BoxOfficeWidgetWorker.PERIODIC_WORK_NAME) }
        verify(exactly = 1) { workManager.cancelUniqueWork(BoxOfficeWidgetWorker.ONE_TIME_WORK_NAME) }
    }
}
