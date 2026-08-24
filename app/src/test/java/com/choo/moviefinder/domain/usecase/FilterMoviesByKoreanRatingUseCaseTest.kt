package com.choo.moviefinder.domain.usecase

import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import com.choo.moviefinder.domain.model.KoreanRating
import com.choo.moviefinder.domain.model.KoreanRatingGrade
import com.choo.moviefinder.domain.model.Movie
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FilterMoviesByKoreanRatingUseCaseTest {

    private lateinit var getKoreanRatingUseCase: GetKoreanRatingUseCase
    private lateinit var useCase: FilterMoviesByKoreanRatingUseCase

    private fun movie(id: Int, title: String) =
        Movie(id, title, "/p.jpg", "/b.jpg", "overview", "2024-01-01", 7.5, 100)

    private fun rating(gradeName: String) =
        KoreanRating(gradeName, "title", "제작사", "감독", "2024", "사유")

    @Before
    fun setUp() {
        getKoreanRatingUseCase = mockk()
        useCase = FilterMoviesByKoreanRatingUseCase(getKoreanRatingUseCase)
    }

    @Test
    fun `invoke returns pagingData unchanged when grade is null`() = runTest {
        val movies = listOf(movie(1, "영화1"), movie(2, "영화2"))
        val pagingData = PagingData.from(movies)

        val result = useCase(pagingData, null)
        val snapshot = flowOf(result).asSnapshot()

        assertEquals(listOf(1, 2), snapshot.map { it.id })
    }

    @Test
    fun `invoke keeps only movies matching selected grade`() = runTest {
        val movies = listOf(movie(1, "12세 영화"), movie(2, "15세 영화"))
        coEvery { getKoreanRatingUseCase("12세 영화") } returns rating("12세이상관람가")
        coEvery { getKoreanRatingUseCase("15세 영화") } returns rating("15세이상관람가")
        val pagingData = PagingData.from(movies)

        val result = useCase(pagingData, KoreanRatingGrade.TWELVE_AND_UP)
        val snapshot = flowOf(result).asSnapshot()

        assertEquals(listOf(1), snapshot.map { it.id })
    }

    @Test
    fun `invoke excludes movies with no rating info`() = runTest {
        val movies = listOf(movie(1, "등급없음"), movie(2, "12세 영화"))
        coEvery { getKoreanRatingUseCase("등급없음") } returns null
        coEvery { getKoreanRatingUseCase("12세 영화") } returns rating("12세이상관람가")
        val pagingData = PagingData.from(movies)

        val result = useCase(pagingData, KoreanRatingGrade.TWELVE_AND_UP)
        val snapshot = flowOf(result).asSnapshot()

        assertEquals(listOf(2), snapshot.map { it.id })
    }

    @Test
    fun `invoke returns empty snapshot when no movies match grade`() = runTest {
        val movies = listOf(movie(1, "12세 영화"))
        coEvery { getKoreanRatingUseCase("12세 영화") } returns rating("12세이상관람가")
        val pagingData = PagingData.from(movies)

        val result = useCase(pagingData, KoreanRatingGrade.RESTRICTED)
        val snapshot = flowOf(result).asSnapshot()

        assertTrue(snapshot.isEmpty())
    }
}
