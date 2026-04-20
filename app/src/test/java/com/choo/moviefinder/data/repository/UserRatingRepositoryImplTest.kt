package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.UserRatingDao
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
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
}
