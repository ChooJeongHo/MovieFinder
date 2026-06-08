package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.RatingCount
import com.choo.moviefinder.data.local.dao.UserRatingDao
import com.choo.moviefinder.data.local.entity.UserRatingEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UserRatingRepositoryImplTest {

    private lateinit var userRatingDao: UserRatingDao
    private lateinit var repository: UserRatingRepositoryImpl

    @Before
    fun setup() {
        userRatingDao = mockk(relaxUnitFun = true)
        repository = UserRatingRepositoryImpl(userRatingDao)
    }

    @Test
    fun `setUserRating with valid inputs delegates to dao`() = runTest {
        repository.setUserRating(1, 4.0f)
        coVerify { userRatingDao.insertRating(match { it.movieId == 1 && it.rating == 4.0f }) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `setUserRating with zero movieId throws`() = runTest {
        repository.setUserRating(0, 3.0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `setUserRating with negative movieId throws`() = runTest {
        repository.setUserRating(-1, 3.0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `setUserRating with rating below 0_5 throws`() = runTest {
        repository.setUserRating(1, 0.4f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `setUserRating with rating above 5_0 throws`() = runTest {
        repository.setUserRating(1, 5.5f)
    }

    @Test
    fun `setUserRating at lower boundary 0_5 succeeds`() = runTest {
        repository.setUserRating(1, 0.5f)
        coVerify { userRatingDao.insertRating(match { it.movieId == 1 && it.rating == 0.5f }) }
    }

    @Test
    fun `setUserRating at upper boundary 5_0 succeeds`() = runTest {
        repository.setUserRating(1, 5.0f)
        coVerify { userRatingDao.insertRating(match { it.movieId == 1 && it.rating == 5.0f }) }
    }

    @Test
    fun `getUserRating delegates to dao`() = runTest {
        every { userRatingDao.getRating(42) } returns flowOf(3.5f)

        val result = repository.getUserRating(42).first()

        assertEquals(3.5f, result)
    }

    @Test
    fun `deleteUserRating delegates to dao`() = runTest {
        coEvery { userRatingDao.deleteRating(5) } returns Unit

        repository.deleteUserRating(5)

        coVerify(exactly = 1) { userRatingDao.deleteRating(5) }
    }

    @Test
    fun `getAverageUserRating delegates to dao`() = runTest {
        every { userRatingDao.getAverageRating() } returns flowOf(4.0f)

        val result = repository.getAverageUserRating().first()

        assertEquals(4.0f, result)
    }

    @Test
    fun `getAllUserRatings maps entities to movieId-rating map`() = runTest {
        every { userRatingDao.getAllRatingsFlow() } returns flowOf(
            listOf(UserRatingEntity(movieId = 1, rating = 3.0f), UserRatingEntity(movieId = 2, rating = 4.5f))
        )

        val result = repository.getAllUserRatings().first()

        assertEquals(2, result.size)
        assertEquals(3.0f, result[1])
        assertEquals(4.5f, result[2])
    }

    @Test
    fun `getRatingDistribution maps RatingCount to RatingBucket`() = runTest {
        every { userRatingDao.getRatingDistribution() } returns flowOf(
            listOf(RatingCount(rating = 4.0f, count = 5))
        )

        val result = repository.getRatingDistribution().first()

        assertEquals(1, result.size)
        assertEquals(4.0f, result[0].rating)
        assertEquals(5, result[0].count)
    }
}
