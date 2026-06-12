package com.choo.moviefinder.data.repository

import androidx.paging.PagingData
import com.choo.moviefinder.core.util.NetworkMonitor
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.CollectionDto
import com.choo.moviefinder.data.remote.dto.MovieDto
import com.choo.moviefinder.domain.model.DomainException
import com.choo.moviefinder.domain.model.Movie
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * MovieRepositoryImpl.getCollection() 및 getUpcomingMovies() 검증.
 * CollectionDto.toDomain()의 날짜 기준 오름차순 정렬 로직을 포함한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MovieRepositoryImplCollectionTest {

    private lateinit var apiService: MovieApiService
    private lateinit var repository: MovieRepositoryImpl

    private fun movieDto(id: Int, title: String, releaseDate: String) = MovieDto(
        id = id,
        title = title,
        posterPath = "/poster$id.jpg",
        backdropPath = null,
        overview = "Overview $id",
        releaseDate = releaseDate,
        voteAverage = 7.0,
        voteCount = 1000
    )

    private val collectionWithMovies = CollectionDto(
        id = 86311,
        name = "The Avengers Collection",
        overview = "Marvel Avengers films.",
        posterPath = "/coll_poster.jpg",
        backdropPath = "/coll_backdrop.jpg",
        parts = listOf(
            movieDto(24428, "The Avengers", "2012-05-04"),
            movieDto(99861, "Avengers: Age of Ultron", "2015-05-01"),
            movieDto(299536, "Avengers: Infinity War", "2018-04-27")
        )
    )

    @Before
    fun setUp() {
        apiService = mockk()
        val networkMonitor = mockk<NetworkMonitor> {
            every { isConnected } returns MutableStateFlow(true)
        }
        repository = MovieRepositoryImpl(
            apiService = apiService,
            database = mockk(),
            cachedMovieDao = mockk(),
            remoteKeyDao = mockk(),
            networkMonitor = networkMonitor
        )
    }

    // --- getCollection ---

    @Test
    fun `getCollection returns mapped CollectionDetail with correct id and name`() = runTest {
        coEvery { apiService.getCollection(86311) } returns collectionWithMovies

        val result = repository.getCollection(86311)

        assertEquals(86311, result.id)
        assertEquals("The Avengers Collection", result.name)
        assertEquals("Marvel Avengers films.", result.overview)
        assertEquals("/coll_poster.jpg", result.posterPath)
        assertEquals("/coll_backdrop.jpg", result.backdropPath)
    }

    @Test
    fun `getCollection returns movies sorted by releaseDate ascending`() = runTest {
        coEvery { apiService.getCollection(86311) } returns collectionWithMovies

        val result = repository.getCollection(86311)

        assertEquals(3, result.movies.size)
        assertEquals("The Avengers", result.movies[0].title) // 2012-05-04
        assertEquals("Avengers: Age of Ultron", result.movies[1].title) // 2015-05-01
        assertEquals("Avengers: Infinity War", result.movies[2].title) // 2018-04-27
    }

    @Test
    fun `getCollection with unordered parts still sorts by releaseDate ascending`() = runTest {
        val unordered = collectionWithMovies.copy(
            parts = listOf(
                movieDto(299536, "Infinity War", "2018-04-27"),
                movieDto(24428, "The Avengers", "2012-05-04"),
                movieDto(99861, "Age of Ultron", "2015-05-01")
            )
        )
        coEvery { apiService.getCollection(86311) } returns unordered

        val result = repository.getCollection(86311)

        assertEquals("The Avengers", result.movies[0].title)
        assertEquals("Age of Ultron", result.movies[1].title)
        assertEquals("Infinity War", result.movies[2].title)
    }

    @Test
    fun `getCollection returns empty movie list when parts is empty`() = runTest {
        coEvery { apiService.getCollection(1) } returns CollectionDto(
            id = 1, name = "Empty Collection", parts = emptyList()
        )

        val result = repository.getCollection(1)

        assertTrue(result.movies.isEmpty())
    }

    @Test
    fun `getCollection delegates to apiService with correct collectionId`() = runTest {
        coEvery { apiService.getCollection(42) } returns CollectionDto(id = 42, name = "Col 42")

        repository.getCollection(42)

        coVerify(exactly = 1) { apiService.getCollection(42) }
    }

    @Test(expected = DomainException.Unknown::class)
    fun `getCollection wraps API exception in DomainException`() = runTest {
        coEvery { apiService.getCollection(1) } throws RuntimeException("Network failure")

        repository.getCollection(1)
    }

    @Test
    fun `getCollection maps single movie correctly`() = runTest {
        coEvery { apiService.getCollection(5) } returns CollectionDto(
            id = 5,
            name = "Solo Collection",
            parts = listOf(movieDto(100, "Standalone", "2021-08-10"))
        )

        val result = repository.getCollection(5)

        assertEquals(1, result.movies.size)
        assertEquals(100, result.movies[0].id)
        assertEquals("Standalone", result.movies[0].title)
        assertEquals("2021-08-10", result.movies[0].releaseDate)
    }

    // --- getUpcomingMovies ---

    @Test
    fun `getUpcomingMovies returns non-null PagingData flow`() {
        val flow: Flow<PagingData<Movie>> = repository.getUpcomingMovies()

        assertNotNull(flow)
    }
}
