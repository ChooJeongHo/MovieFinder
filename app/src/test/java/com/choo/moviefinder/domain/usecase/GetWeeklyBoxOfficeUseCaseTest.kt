package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.BoxOffice
import com.choo.moviefinder.domain.model.BoxOfficeWeekType
import com.choo.moviefinder.domain.repository.BoxOfficeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetWeeklyBoxOfficeUseCaseTest {

    private lateinit var repository: BoxOfficeRepository
    private lateinit var useCase: GetWeeklyBoxOfficeUseCase

    private val boxOfficeList = listOf(
        BoxOffice(1, 0, false, "20233219", "호프", "2026-07-15", 1286065L, 3415701L, 13_526_359_400L, 2258)
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetWeeklyBoxOfficeUseCase(repository)
    }

    @Test
    fun `invoke with explicit targetDate and weekType delegates to repository unchanged`() = runTest {
        coEvery { repository.getWeeklyBoxOffice("20260726", BoxOfficeWeekType.WEEK) } returns boxOfficeList

        val result = useCase("20260726", BoxOfficeWeekType.WEEK)

        coVerify(exactly = 1) { repository.getWeeklyBoxOffice("20260726", BoxOfficeWeekType.WEEK) }
        assertEquals(boxOfficeList, result)
    }

    @Test
    fun `invoke without targetDate defaults to yyyyMMdd formatted date`() = runTest {
        val captured = mutableListOf<String>()
        coEvery { repository.getWeeklyBoxOffice(capture(captured), BoxOfficeWeekType.WEEK) } returns boxOfficeList

        useCase()

        assertTrue(Regex("^\\d{8}$").matches(captured.single()))
    }

    @Test
    fun `invoke defaults weekType to WEEK`() = runTest {
        coEvery { repository.getWeeklyBoxOffice(any(), BoxOfficeWeekType.WEEK) } returns boxOfficeList

        useCase()

        coVerify(exactly = 1) { repository.getWeeklyBoxOffice(any(), BoxOfficeWeekType.WEEK) }
        coVerify(exactly = 0) { repository.getWeeklyBoxOffice(any(), BoxOfficeWeekType.WEEKEND) }
        coVerify(exactly = 0) { repository.getWeeklyBoxOffice(any(), BoxOfficeWeekType.WEEKDAY) }
    }

    @Test
    fun `invoke forwards WEEKEND week type unchanged`() = runTest {
        coEvery { repository.getWeeklyBoxOffice("20260726", BoxOfficeWeekType.WEEKEND) } returns emptyList()

        useCase("20260726", BoxOfficeWeekType.WEEKEND)

        coVerify(exactly = 1) { repository.getWeeklyBoxOffice("20260726", BoxOfficeWeekType.WEEKEND) }
    }

    @Test
    fun `invoke returns empty list when repository has no data`() = runTest {
        coEvery { repository.getWeeklyBoxOffice("20260726", BoxOfficeWeekType.WEEK) } returns emptyList()

        val result = useCase("20260726", BoxOfficeWeekType.WEEK)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke propagates repository exception`() = runTest {
        coEvery { repository.getWeeklyBoxOffice("20260726", BoxOfficeWeekType.WEEK) } throws
            java.io.IOException("network down")

        var thrown: Throwable? = null
        try {
            useCase("20260726", BoxOfficeWeekType.WEEK)
        } catch (e: java.io.IOException) {
            thrown = e
        }

        assertEquals("network down", thrown?.message)
    }
}
