package com.choo.moviefinder.core.notification

import android.content.Context
import android.os.Build
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

@OptIn(ExperimentalCoroutinesApi::class)
class ReleaseNotificationSchedulerTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: ReleaseNotificationScheduler

    @Before
    fun setup() {
        // Set SDK_INT to 26 (Android O) so the API level guard does not return early.
        // JDK 12+ removed Field.modifiers; use sun.misc.Unsafe to write the final static field.
        setSdkInt(26)

        context = mockk(relaxed = true)
        workManager = mockk(relaxed = true)

        // Mock the Kotlin companion object so getInstance() never executes real WorkManager code.
        // mockkObject intercepts Kotlin object/companion calls without running the lambda body.
        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any()) } returns workManager

        every { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) } returns mockk(relaxed = true)
        every { workManager.cancelUniqueWork(any()) } returns mockk(relaxed = true)

        scheduler = ReleaseNotificationScheduler(context)
    }

    @After
    fun tearDown() {
        unmockkObject(WorkManager.Companion)
        setSdkInt(0)
    }

    // sun.misc.Unsafe is the only reliable way to write a final static field on JDK 12+
    @Suppress("DEPRECATION")
    private fun setSdkInt(value: Int) {
        val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null) as sun.misc.Unsafe

        val sdkIntField = Build.VERSION::class.java.getField("SDK_INT")
        val base = unsafe.staticFieldBase(sdkIntField)
        val offset = unsafe.staticFieldOffset(sdkIntField)
        unsafe.putInt(base, offset, value)
    }

    @Test
    fun `schedule enqueues work for future release date`() = runTest {
        scheduler.schedule(movieId = 1, movieTitle = "Test Movie", releaseDate = "2099-12-31")

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
        }
    }

    @Test
    fun `schedule skips past release date`() = runTest {
        scheduler.schedule(movieId = 1, movieTitle = "Old Movie", releaseDate = "2000-01-01")

        verify(exactly = 0) {
            workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
        }
    }

    @Test
    fun `schedule skips invalid date format`() = runTest {
        scheduler.schedule(movieId = 1, movieTitle = "Bad Movie", releaseDate = "invalid-date")

        verify(exactly = 0) {
            workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
        }
    }

    @Test
    fun `schedule skips empty release date`() = runTest {
        scheduler.schedule(movieId = 1, movieTitle = "No Date Movie", releaseDate = "")

        verify(exactly = 0) {
            workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
        }
    }

    @Test
    fun `cancel cancels unique work with correct name`() = runTest {
        val movieId = 42
        scheduler.cancel(movieId)

        verify(exactly = 1) { workManager.cancelUniqueWork("release_$movieId") }
    }

    @Test
    fun `schedule uses correct work name`() = runTest {
        val movieId = 99
        val workNameSlot = slot<String>()
        every {
            workManager.enqueueUniqueWork(capture(workNameSlot), any(), any<OneTimeWorkRequest>())
        } returns mockk(relaxed = true)

        scheduler.schedule(movieId = movieId, movieTitle = "Named Movie", releaseDate = "2099-06-15")

        assert(workNameSlot.captured == "release_$movieId") {
            "Expected work name 'release_$movieId' but was '${workNameSlot.captured}'"
        }
    }

    @Test
    fun `schedule uses ExistingWorkPolicy KEEP`() = runTest {
        val policySlot = slot<ExistingWorkPolicy>()
        every {
            workManager.enqueueUniqueWork(any(), capture(policySlot), any<OneTimeWorkRequest>())
        } returns mockk(relaxed = true)

        scheduler.schedule(movieId = 7, movieTitle = "Policy Movie", releaseDate = "2099-03-20")

        assert(policySlot.captured == ExistingWorkPolicy.KEEP) {
            "Expected ExistingWorkPolicy.KEEP but was ${policySlot.captured}"
        }
    }
}
