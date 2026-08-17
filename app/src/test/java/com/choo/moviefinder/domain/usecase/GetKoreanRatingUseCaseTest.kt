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
    fun `invoke returns matching rating for exact normalized title`() = runTest {
        coEvery { repository.searchRatings("테스트 영화") } returns listOf(rating("테스트 영화"))

        val result = useCase("테스트 영화")

        assertEquals("테스트 영화", result?.title)
    }

    @Test
    fun `invoke matches titles differing only by whitespace and punctuation`() = runTest {
        coEvery { repository.searchRatings("탑건: 매버릭") } returns listOf(rating("탑건:매버릭"))

        val result = useCase("탑건: 매버릭")

        assertEquals("탑건:매버릭", result?.title)
    }

    @Test
    fun `invoke matches titles differing only by case for English titles`() = runTest {
        coEvery { repository.searchRatings("Top Gun") } returns listOf(rating("TOP GUN"))

        val result = useCase("Top Gun")

        assertEquals("TOP GUN", result?.title)
    }

    @Test
    fun `invoke returns null for partial containment match (conservative matching)`() = runTest {
        coEvery { repository.searchRatings("인터스텔라") } returns listOf(rating("인터스텔라 리마스터링"))

        val result = useCase("인터스텔라")

        assertNull(result)
    }

    @Test
    fun `invoke returns first matching candidate when multiple candidates match`() = runTest {
        coEvery { repository.searchRatings("동명 영화") } returns
            listOf(rating("동명 영화", "12세이상관람가"), rating("동명 영화", "15세이상관람가"))

        val result = useCase("동명 영화")

        assertEquals("12세이상관람가", result?.gradeName)
    }

    @Test
    fun `invoke returns null when repository returns empty list`() = runTest {
        coEvery { repository.searchRatings("결과없음") } returns emptyList()

        val result = useCase("결과없음")

        assertNull(result)
    }

    @Test
    fun `invoke returns null without propagating when repository throws DomainException`() = runTest {
        coEvery { repository.searchRatings("네트워크실패") } throws DomainException.NetworkError(RuntimeException())

        val result = useCase("네트워크실패")

        assertNull(result)
    }

    @Test(expected = CancellationException::class)
    fun `invoke rethrows CancellationException from repository`() = runTest {
        coEvery { repository.searchRatings("취소됨") } throws CancellationException("cancelled")

        useCase("취소됨")
    }

    @Test
    fun `invoke does not call repository for blank title and returns null`() = runTest {
        val result = useCase("   ")

        assertNull(result)
        coVerify(exactly = 0) { repository.searchRatings(any(), any(), any()) }
    }
}
