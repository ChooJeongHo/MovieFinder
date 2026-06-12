package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.MovieDto
import com.choo.moviefinder.data.remote.dto.PersonCreditsResponse
import com.choo.moviefinder.domain.model.DomainException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * cast + crew merge, distinctBy(id), sortedByDescending(releaseDate) 로직을 집중 검증한다.
 * 기본 단순 위임 케이스는 PersonRepositoryImplTest에서 커버됨.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PersonRepositoryImplCreditsTest {

    private lateinit var apiService: MovieApiService
    private lateinit var repository: PersonRepositoryImpl

    private fun movieDto(id: Int, title: String, releaseDate: String = "2020-01-01") = MovieDto(
        id = id,
        title = title,
        posterPath = null,
        backdropPath = null,
        overview = "",
        releaseDate = releaseDate,
        voteAverage = 7.0,
        voteCount = 100
    )

    @Before
    fun setUp() {
        apiService = mockk()
        repository = PersonRepositoryImpl(apiService)
    }

    // --- cast + crew merge ---

    @Test
    fun `getPersonMovieCredits merges cast and crew into single list`() = runTest {
        coEvery { apiService.getPersonMovieCredits(1) } returns PersonCreditsResponse(
            cast = listOf(movieDto(10, "Cast Movie")),
            crew = listOf(movieDto(20, "Crew Movie"))
        )

        val result = repository.getPersonMovieCredits(1)

        assertEquals(2, result.size)
        val ids = result.map { it.id }.toSet()
        assertTrue(10 in ids)
        assertTrue(20 in ids)
    }

    @Test
    fun `getPersonMovieCredits deduplicates movies appearing in both cast and crew`() = runTest {
        val sharedMovie = movieDto(99, "Shared Movie", "2022-06-15")
        coEvery { apiService.getPersonMovieCredits(1) } returns PersonCreditsResponse(
            cast = listOf(sharedMovie),
            crew = listOf(sharedMovie)
        )

        val result = repository.getPersonMovieCredits(1)

        assertEquals(1, result.size)
        assertEquals(99, result[0].id)
    }

    @Test
    fun `getPersonMovieCredits keeps only one entry when same id appears multiple times`() = runTest {
        coEvery { apiService.getPersonMovieCredits(1) } returns PersonCreditsResponse(
            cast = listOf(
                movieDto(5, "Movie A"),
                movieDto(5, "Movie A duplicate")
            ),
            crew = listOf(movieDto(5, "Movie A crew"))
        )

        val result = repository.getPersonMovieCredits(1)

        assertEquals(1, result.size)
        assertEquals(5, result[0].id)
    }

    // --- sortedByDescending(releaseDate) ---

    @Test
    fun `getPersonMovieCredits returns movies sorted by releaseDate descending`() = runTest {
        coEvery { apiService.getPersonMovieCredits(1) } returns PersonCreditsResponse(
            cast = listOf(
                movieDto(1, "Oldest", "2010-03-01"),
                movieDto(2, "Newest", "2023-11-15"),
                movieDto(3, "Middle", "2017-07-04")
            )
        )

        val result = repository.getPersonMovieCredits(1)

        assertEquals(listOf("Newest", "Middle", "Oldest"), result.map { it.title })
    }

    @Test
    fun `getPersonMovieCredits places crew-only movies correctly in date order`() = runTest {
        coEvery { apiService.getPersonMovieCredits(1) } returns PersonCreditsResponse(
            cast = listOf(movieDto(10, "Cast Only", "2015-05-01")),
            crew = listOf(movieDto(20, "Crew Only", "2020-01-01"))
        )

        val result = repository.getPersonMovieCredits(1)

        assertEquals(2, result.size)
        assertEquals("Crew Only", result[0].title)
        assertEquals("Cast Only", result[1].title)
    }

    // --- edge cases ---

    @Test
    fun `getPersonMovieCredits returns empty list when both cast and crew are empty`() = runTest {
        coEvery { apiService.getPersonMovieCredits(1) } returns PersonCreditsResponse(
            cast = emptyList(),
            crew = emptyList()
        )

        val result = repository.getPersonMovieCredits(1)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getPersonMovieCredits returns crew movies when cast is empty`() = runTest {
        coEvery { apiService.getPersonMovieCredits(1) } returns PersonCreditsResponse(
            cast = emptyList(),
            crew = listOf(movieDto(30, "Director Movie", "2018-09-20"))
        )

        val result = repository.getPersonMovieCredits(1)

        assertEquals(1, result.size)
        assertEquals(30, result[0].id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getPersonMovieCredits throws IllegalArgumentException for zero personId`() = runTest {
        repository.getPersonMovieCredits(0)
    }

    @Test(expected = DomainException.Unknown::class)
    fun `getPersonMovieCredits wraps API exception in DomainException`() = runTest {
        coEvery { apiService.getPersonMovieCredits(1) } throws RuntimeException("API error")

        repository.getPersonMovieCredits(1)
    }
}
