package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.ReviewFeedbackRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReviewFeedbackUseCasesTest {

    private lateinit var repository: ReviewFeedbackRepository

    @Before
    fun setUp() {
        repository = mockk()
    }

    // --- GetHelpfulReviewIdsUseCase ---

    @Test
    fun `GetHelpfulReviewIdsUseCase returns helpful review ids from repository`() = runTest {
        coEvery { repository.getHelpfulReviewIds(1) } returns setOf("r1", "r2")
        val useCase = GetHelpfulReviewIdsUseCase(repository)

        val result = useCase(1)

        assertEquals(setOf("r1", "r2"), result)
    }

    @Test
    fun `GetHelpfulReviewIdsUseCase passes correct movieId`() = runTest {
        coEvery { repository.getHelpfulReviewIds(any()) } returns emptySet()
        val useCase = GetHelpfulReviewIdsUseCase(repository)

        useCase(42)

        coVerify { repository.getHelpfulReviewIds(42) }
    }

    // --- ToggleReviewHelpfulUseCase ---

    @Test
    fun `ToggleReviewHelpfulUseCase marks review helpful`() = runTest {
        coEvery { repository.setHelpful(1, "r1", true) } returns Unit
        val useCase = ToggleReviewHelpfulUseCase(repository)

        useCase(1, "r1", true)

        coVerify { repository.setHelpful(1, "r1", true) }
    }

    @Test
    fun `ToggleReviewHelpfulUseCase unmarks review helpful`() = runTest {
        coEvery { repository.setHelpful(1, "r1", false) } returns Unit
        val useCase = ToggleReviewHelpfulUseCase(repository)

        useCase(1, "r1", false)

        coVerify { repository.setHelpful(1, "r1", false) }
    }
}
