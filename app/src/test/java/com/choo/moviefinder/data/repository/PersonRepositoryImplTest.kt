package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.KnownForMovie
import com.choo.moviefinder.data.remote.dto.MovieDto
import com.choo.moviefinder.data.remote.dto.PersonCreditsResponse
import com.choo.moviefinder.data.remote.dto.PersonDetailDto
import com.choo.moviefinder.data.remote.dto.PersonSearchResponse
import com.choo.moviefinder.data.remote.dto.PersonSearchResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersonRepositoryImplTest {

    private lateinit var apiService: MovieApiService
    private lateinit var repository: PersonRepositoryImpl

    private val testPersonDetailDto = PersonDetailDto(
        id = 6193,
        name = "Leonardo DiCaprio",
        biography = "An actor.",
        birthday = "1974-11-11",
        deathday = null,
        placeOfBirth = "Los Angeles, CA",
        profilePath = "/profile.jpg",
        knownForDepartment = "Acting"
    )

    private val testPersonSearchResult = PersonSearchResult(
        id = 6193,
        name = "Leonardo DiCaprio",
        profilePath = "/profile.jpg",
        knownForDepartment = "Acting",
        knownFor = listOf(
            KnownForMovie(id = 1, title = "Inception"),
            KnownForMovie(id = 2, title = "Titanic")
        )
    )

    private val testMovieDto = MovieDto(
        id = 27205,
        title = "Inception",
        posterPath = "/poster.jpg",
        backdropPath = null,
        overview = "A dream within a dream.",
        releaseDate = "2010-07-16",
        voteAverage = 8.8,
        voteCount = 30000,
        genreIds = listOf(28, 878)
    )

    @Before
    fun setup() {
        apiService = mockk()
        repository = PersonRepositoryImpl(apiService)
    }

    @Test
    fun `getPersonDetail - returns mapped domain model`() = runTest {
        coEvery { apiService.getPersonDetail(6193) } returns testPersonDetailDto

        val result = repository.getPersonDetail(6193)

        assertEquals(6193, result.id)
        assertEquals("Leonardo DiCaprio", result.name)
        assertEquals("1974-11-11", result.birthday)
        assertNull(result.deathday)
        assertEquals("/profile.jpg", result.profilePath)
        assertEquals("Acting", result.knownForDepartment)
    }

    @Test
    fun `getPersonDetail - delegates to apiService`() = runTest {
        coEvery { apiService.getPersonDetail(6193) } returns testPersonDetailDto

        repository.getPersonDetail(6193)

        coVerify(exactly = 1) { apiService.getPersonDetail(6193) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getPersonDetail - negative id throws IllegalArgumentException`() = runTest {
        repository.getPersonDetail(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getPersonDetail - zero id throws IllegalArgumentException`() = runTest {
        repository.getPersonDetail(0)
    }

    @Test
    fun `getPersonMovieCredits - returns list of domain movies`() = runTest {
        coEvery { apiService.getPersonMovieCredits(6193) } returns
            PersonCreditsResponse(cast = listOf(testMovieDto))

        val result = repository.getPersonMovieCredits(6193)

        assertEquals(1, result.size)
        assertEquals(27205, result[0].id)
        assertEquals("Inception", result[0].title)
    }

    @Test
    fun `getPersonMovieCredits - empty cast returns empty list`() = runTest {
        coEvery { apiService.getPersonMovieCredits(6193) } returns
            PersonCreditsResponse(cast = emptyList())

        val result = repository.getPersonMovieCredits(6193)

        assertEquals(0, result.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getPersonMovieCredits - negative id throws`() = runTest {
        repository.getPersonMovieCredits(-1)
    }

    @Test
    fun `searchPerson - returns mapped domain items`() = runTest {
        coEvery { apiService.searchPerson("Leonardo") } returns
            PersonSearchResponse(results = listOf(testPersonSearchResult))

        val result = repository.searchPerson("Leonardo")

        assertEquals(1, result.size)
        assertEquals(6193, result[0].id)
        assertEquals("Leonardo DiCaprio", result[0].name)
        assertEquals("Inception, Titanic", result[0].knownForTitles)
    }

    @Test
    fun `searchPerson - empty results returns empty list`() = runTest {
        coEvery { apiService.searchPerson("unknown") } returns
            PersonSearchResponse(results = emptyList())

        val result = repository.searchPerson("unknown")

        assertEquals(0, result.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `searchPerson - blank query throws`() = runTest {
        repository.searchPerson("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `searchPerson - empty query throws`() = runTest {
        repository.searchPerson("")
    }

    @Test
    fun `searchPerson - knownForTitles capped at 3`() = runTest {
        val manyMovies = (1..5).map { KnownForMovie(id = it, title = "Movie $it") }
        coEvery { apiService.searchPerson("actor") } returns
            PersonSearchResponse(
                results = listOf(testPersonSearchResult.copy(knownFor = manyMovies))
            )

        val result = repository.searchPerson("actor")

        assertEquals(3, result[0].knownForTitles.split(", ").size)
    }
}
