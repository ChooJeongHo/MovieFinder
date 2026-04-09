package com.choo.moviefinder.core.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.choo.moviefinder.core.notification.ReleaseNotificationWorker.Companion.KEY_MOVIE_ID
import com.choo.moviefinder.core.notification.ReleaseNotificationWorker.Companion.KEY_MOVIE_TITLE
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReleaseNotificationWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
    }

    private fun buildWorker(movieId: Int, movieTitle: String?): ReleaseNotificationWorker {
        val data = Data.Builder()
            .putInt(KEY_MOVIE_ID, movieId)
            .apply { movieTitle?.let { putString(KEY_MOVIE_TITLE, it) } }
            .build()
        every { workerParams.inputData } returns data
        return ReleaseNotificationWorker(context, workerParams)
    }

    @Test
    fun `missing movie title - returns failure`() {
        val worker = buildWorker(movieId = 550, movieTitle = null)
        assertEquals(Result.failure(), worker.doWork())
    }

    @Test
    fun `invalid movie id minus one - returns failure`() {
        val worker = buildWorker(movieId = -1, movieTitle = "Fight Club")
        assertEquals(Result.failure(), worker.doWork())
    }

    @Test
    fun `key constants are correct`() {
        assertEquals("movie_id", KEY_MOVIE_ID)
        assertEquals("movie_title", KEY_MOVIE_TITLE)
    }
}
