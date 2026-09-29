package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.DomainException
import com.choo.moviefinder.domain.repository.MovieDetailRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GetKoreanTitleUseCaseTest {

    private lateinit var repository: MovieDetailRepository
    private lateinit var useCase: GetKoreanTitleUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetKoreanTitleUseCase(repository)
    }

    @Test
    fun `invoke returns korean title from repository`() = runTest {
        coEvery { repository.getKoreanTitle(603) } returns "매트릭스"

        val result = useCase(603)

        assertEquals("매트릭스", result)
    }

    @Test
    fun `invoke returns null without propagating when repository throws DomainException`() = runTest {
        coEvery { repository.getKoreanTitle(603) } throws DomainException.NetworkError(RuntimeException())

        val result = useCase(603)

        assertNull(result)
    }

    @Test(expected = CancellationException::class)
    fun `invoke rethrows CancellationException from repository`() = runTest {
        coEvery { repository.getKoreanTitle(603) } throws CancellationException("cancelled")

        useCase(603)
    }
}
