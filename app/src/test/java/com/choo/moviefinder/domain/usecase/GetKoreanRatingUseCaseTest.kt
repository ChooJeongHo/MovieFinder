package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.DomainException
import com.choo.moviefinder.domain.model.KoreanRating
import com.choo.moviefinder.domain.repository.KoreanRatingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GetKoreanRatingUseCaseTest {

    private lateinit var repository: KoreanRatingRepository
    private lateinit var useCase: GetKoreanRatingUseCase

    private fun rating(title: String, gradeName: String = "15세이상관람가") =
        KoreanRating(gradeName, title, "제작사", "감독", "2024", "사유")

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetKoreanRatingUseCase(repository)
    }

    @Test
    fun `invoke returns rating from repository`() = runTest {
        coEvery { repository.getRatingForMovie("테스트 영화") } returns rating("테스트 영화")

        val result = useCase("테스트 영화")

        assertEquals("테스트 영화", result?.title)
    }

    @Test
    fun `invoke returns null when repository returns null`() = runTest {
        coEvery { repository.getRatingForMovie("결과없음") } returns null

        val result = useCase("결과없음")

        assertNull(result)
    }

    @Test
    fun `invoke returns null without propagating when repository throws DomainException`() = runTest {
        coEvery { repository.getRatingForMovie("네트워크실패") } throws DomainException.NetworkError(RuntimeException())

        val result = useCase("네트워크실패")

        assertNull(result)
    }

    @Test(expected = CancellationException::class)
    fun `invoke rethrows CancellationException from repository`() = runTest {
        coEvery { repository.getRatingForMovie("취소됨") } throws CancellationException("cancelled")

        useCase("취소됨")
    }

    @Test
    fun `invoke does not call repository for blank title and returns null`() = runTest {
        val result = useCase("   ")

        assertNull(result)
        coVerify(exactly = 0) { repository.getRatingForMovie(any()) }
    }
}
