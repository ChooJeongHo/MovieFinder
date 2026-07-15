package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.HelpfulReviewDao
import com.choo.moviefinder.data.local.entity.HelpfulReviewEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReviewFeedbackRepositoryImplTest {

    private lateinit var helpfulReviewDao: HelpfulReviewDao
    private lateinit var repository: ReviewFeedbackRepositoryImpl

    @Before
    fun setup() {
        helpfulReviewDao = mockk(relaxUnitFun = true)
        repository = ReviewFeedbackRepositoryImpl(helpfulReviewDao)
    }

    @Test
    fun `getHelpfulReviewIds converts dao list to set`() = runTest {
        coEvery { helpfulReviewDao.getHelpfulReviewIds(1) } returns listOf("r1", "r2")

        val result = repository.getHelpfulReviewIds(1)

        assertEquals(setOf("r1", "r2"), result)
    }

    @Test
    fun `setHelpful true marks review with movieId and reviewId`() = runTest {
        repository.setHelpful(1, "r1", true)

        coVerify {
            helpfulReviewDao.mark(match<HelpfulReviewEntity> { it.movieId == 1 && it.reviewId == "r1" })
        }
    }

    @Test
    fun `setHelpful false unmarks review by reviewId`() = runTest {
        repository.setHelpful(1, "r1", false)

        coVerify { helpfulReviewDao.unmark("r1") }
        coVerify(exactly = 0) { helpfulReviewDao.mark(any()) }
    }
}
