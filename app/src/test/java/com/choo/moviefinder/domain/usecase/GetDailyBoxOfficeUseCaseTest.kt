package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.BoxOffice
import com.choo.moviefinder.domain.repository.BoxOfficeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetDailyBoxOfficeUseCaseTest {

    private lateinit var repository: BoxOfficeRepository
    private lateinit var useCase: GetDailyBoxOfficeUseCase

    private val boxOfficeList = listOf(
        BoxOffice(1, 0, false, "20240001", "영화 1", "2024-01-01", 1000L, 5000L, 10_000_000L, 100)
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetDailyBoxOfficeUseCase(repository)
    }

    @Test
    fun `invoke with explicit targetDate delegates to repository unchanged`() = runTest {
        coEvery { repository.getDailyBoxOffice("20240315") } returns boxOfficeList

        val result = useCase("20240315")

        coVerify(exactly = 1) { repository.getDailyBoxOffice("20240315") }
        assertEquals(boxOfficeList, result)
    }

    @Test
    fun `invoke without targetDate defaults to yesterday in yyyyMMdd format`() = runTest {
        val slot = mutableListOf<String>()
        coEvery { repository.getDailyBoxOffice(capture(slot)) } returns boxOfficeList

        useCase()

        assertTrue(Regex("^\\d{8}$").matches(slot.single()))
    }

    @Test
    fun `invoke returns empty list when repository has no data`() = runTest {
        coEvery { repository.getDailyBoxOffice(any()) } returns emptyList()

        val result = useCase("20240101")

        assertTrue(result.isEmpty())
    }
}
