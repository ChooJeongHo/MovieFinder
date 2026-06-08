package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.WatchProvider
import com.choo.moviefinder.domain.repository.MovieDetailRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetWatchProvidersUseCaseTest {

    private lateinit var repository: MovieDetailRepository
    private lateinit var useCase: GetWatchProvidersUseCase

    private val providers = listOf(
        WatchProvider(providerId = 8, providerName = "Netflix", logoPath = "/netflix.png"),
        WatchProvider(providerId = 337, providerName = "Disney+", logoPath = "/disney.png")
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetWatchProvidersUseCase(repository)
    }

    @Test
    fun `invoke delegates to repository getWatchProviders with correct movieId`() = runTest {
        coEvery { repository.getWatchProviders(550) } returns providers

        useCase(550)

        coVerify(exactly = 1) { repository.getWatchProviders(550) }
    }

    @Test
    fun `invoke returns providers from repository`() = runTest {
        coEvery { repository.getWatchProviders(550) } returns providers

        val result = useCase(550)

        assertEquals(providers, result)
    }

    @Test
    fun `invoke returns empty list when no providers available`() = runTest {
        coEvery { repository.getWatchProviders(any()) } returns emptyList()

        val result = useCase(1)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke passes the correct movieId to repository`() = runTest {
        coEvery { repository.getWatchProviders(999) } returns providers

        useCase(999)

        coVerify(exactly = 1) { repository.getWatchProviders(999) }
        coVerify(exactly = 0) { repository.getWatchProviders(550) }
    }

    @Test
    fun `invoke returns provider with null logoPath`() = runTest {
        val providerWithNullLogo = listOf(WatchProvider(1, "Unknown", null))
        coEvery { repository.getWatchProviders(any()) } returns providerWithNullLogo

        val result = useCase(1)

        assertEquals(1, result.size)
        assertEquals(null, result[0].logoPath)
    }
}
