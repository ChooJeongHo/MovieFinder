package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieQueryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class InvalidateHomeMovieCacheUseCaseTest {

    private lateinit var repository: MovieQueryRepository
    private lateinit var useCase: InvalidateHomeMovieCacheUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = InvalidateHomeMovieCacheUseCase(repository)
    }

    @Test
    fun `invoke delegates to repository`() = runTest {
        coEvery { repository.invalidateHomeMovieCache() } returns Unit

        useCase()

        coVerify(exactly = 1) { repository.invalidateHomeMovieCache() }
    }
}
