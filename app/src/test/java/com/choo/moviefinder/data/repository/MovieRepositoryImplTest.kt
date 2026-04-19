package com.choo.moviefinder.data.repository

import com.choo.moviefinder.core.util.NetworkMonitor
import com.choo.moviefinder.data.local.MovieDatabase
import com.choo.moviefinder.data.local.dao.CachedMovieDao
import com.choo.moviefinder.data.local.dao.RemoteKeyDao
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.CastDto
import com.choo.moviefinder.data.remote.dto.CreditsResponse
import com.choo.moviefinder.data.remote.dto.GenreDto
import com.choo.moviefinder.data.remote.dto.GenreListResponse
import com.choo.moviefinder.data.remote.dto.MovieDetailDto
import com.choo.moviefinder.data.remote.dto.MovieDto
import com.choo.moviefinder.data.remote.dto.MovieListResponse
import com.choo.moviefinder.data.remote.dto.ReleaseDateInfo
import com.choo.moviefinder.data.remote.dto.ReleaseDateResponse
import com.choo.moviefinder.data.remote.dto.ReleaseDateResult
import com.choo.moviefinder.data.remote.dto.AuthorDetailsDto
import com.choo.moviefinder.data.remote.dto.ReviewDto
import com.choo.moviefinder.data.remote.dto.ReviewResponse
import com.choo.moviefinder.data.remote.dto.VideoDto
import com.choo.moviefinder.data.remote.dto.VideoResponse
import androidx.paging.PagingData
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.presentation.search.SortOption
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieRepositoryImplTest {

    private lateinit var apiService: MovieApiService
    private lateinit var database: MovieDatabase
    private lateinit var cachedMovieDao: CachedMovieDao
    private lateinit var remoteKeyDao: RemoteKeyDao
    private lateinit var networkMonitor: NetworkMonitor

    private lateinit var repository: MovieRepositoryImpl

    private val testMovieDetailDto = MovieDetailDto(
        id = 1,
        title = "Test Movie",
        posterPath = "/poster.jpg",
        backdropPath = "/backdrop.jpg",
        overview = "Test overview",
        releaseDate = "2024-01-01",
        voteAverage = 8.5,
        voteCount = 1000,
        runtime = 120,
        genres = listOf(GenreDto(1, "Action"), GenreDto(2, "Drama")),
        tagline = "Test tagline"
    )

    private val testCastDtos = listOf(
        CastDto(id = 1, name = "Actor 1", character = "Character 1", profilePath = "/p1.jpg"),
        CastDto(id = 2, name = "Actor 2", character = "Character 2", profilePath = "/p2.jpg")
    )

    private val testMovieDtos = listOf(
        MovieDto(
            id = 10, title = "Similar 1", posterPath = "/s1.jpg",
            overview = "Overview 1", releaseDate = "2024-02-01",
            voteAverage = 7.0, voteCount = 500
        ),
        MovieDto(
            id = 11, title = "Similar 2", posterPath = "/s2.jpg",
            overview = "Overview 2", releaseDate = "2024-03-01",
            voteAverage = 6.5, voteCount = 300
        )
    )

    @Before
    fun setup() {
        apiService = mockk()
        database = mockk()
        cachedMovieDao = mockk()
        remoteKeyDao = mockk()
        networkMonitor = mockk {
            every { isConnected } returns MutableStateFlow(true)
        }

        repository = MovieRepositoryImpl(
            apiService = apiService,
            database = database,
            cachedMovieDao = cachedMovieDao,
            remoteKeyDao = remoteKeyDao,
            networkMonitor = networkMonitor
        )
    }

    // --- getMovieDetail ---

    @Test
    fun `getMovieDetail returns mapped domain model`() = runTest {
        coEvery { apiService.getMovieDetail(1) } returns testMovieDetailDto

        val result = repository.getMovieDetail(1)

        assertEquals(1, result.id)
        assertEquals("Test Movie", result.title)
        assertEquals("/poster.jpg", result.posterPath)
        assertEquals("/backdrop.jpg", result.backdropPath)
        assertEquals("Test overview", result.overview)
        assertEquals(8.5, result.voteAverage, 0.01)
        assertEquals(120, result.runtime)
        assertEquals(2, result.genres.size)
        assertEquals("Action", result.genres[0].name)
        assertEquals("Test tagline", result.tagline)
    }

    @Test(expected = RuntimeException::class)
    fun `getMovieDetail propagates exception on API failure`() = runTest {
        coEvery { apiService.getMovieDetail(1) } throws RuntimeException("Network error")

        repository.getMovieDetail(1)
    }

    // --- getMovieCredits ---

    @Test
    fun `getMovieCredits returns mapped cast list`() = runTest {
        coEvery { apiService.getMovieCredits(1) } returns CreditsResponse(id = 1, cast = testCastDtos)

        val result = repository.getMovieCredits(1)

        assertEquals(2, result.size)
        assertEquals("Actor 1", result[0].name)
        assertEquals("Character 1", result[0].character)
        assertEquals("/p1.jpg", result[0].profilePath)
        assertEquals("Actor 2", result[1].name)
    }

    @Test
    fun `getMovieCredits returns empty list when no cast`() = runTest {
        coEvery { apiService.getMovieCredits(1) } returns CreditsResponse(id = 1, cast = emptyList())

        val result = repository.getMovieCredits(1)

        assertTrue(result.isEmpty())
    }

    // --- getSimilarMovies ---

    @Test
    fun `getSimilarMovies returns mapped movie list`() = runTest {
        coEvery { apiService.getSimilarMovies(1) } returns MovieListResponse(
            page = 1, results = testMovieDtos, totalPages = 1, totalResults = 2
        )

        val result = repository.getSimilarMovies(1)

        assertEquals(2, result.size)
        assertEquals("Similar 1", result[0].title)
        assertEquals(10, result[0].id)
        assertEquals("Similar 2", result[1].title)
    }

    // --- getMovieTrailerKey ---

    @Test
    fun `getMovieTrailerKey returns official YouTube trailer key`() = runTest {
        val videos = listOf(
            VideoDto(key = "abc123", site = "YouTube", type = "Trailer", official = true),
            VideoDto(key = "def456", site = "YouTube", type = "Trailer", official = false),
            VideoDto(key = "ghi789", site = "Vimeo", type = "Trailer", official = true)
        )
        coEvery { apiService.getMovieVideos(1) } returns VideoResponse(id = 1, results = videos)

        val result = repository.getMovieTrailerKey(1)

        assertEquals("abc123", result)
    }

    @Test
    fun `getMovieTrailerKey prefers official over unofficial`() = runTest {
        val videos = listOf(
            VideoDto(key = "unofficial", site = "YouTube", type = "Trailer", official = false),
            VideoDto(key = "official", site = "YouTube", type = "Trailer", official = true)
        )
        coEvery { apiService.getMovieVideos(1) } returns VideoResponse(id = 1, results = videos)

        val result = repository.getMovieTrailerKey(1)

        assertEquals("official", result)
    }

    @Test
    fun `getMovieTrailerKey falls back to non-trailer YouTube video`() = runTest {
        val videos = listOf(
            VideoDto(key = "teaser123", site = "YouTube", type = "Teaser", official = true),
            VideoDto(key = "vimeo", site = "Vimeo", type = "Trailer", official = true)
        )
        coEvery { apiService.getMovieVideos(1) } returns VideoResponse(id = 1, results = videos)

        val result = repository.getMovieTrailerKey(1)

        assertEquals("teaser123", result)
    }

    @Test
    fun `getMovieTrailerKey returns null when no YouTube videos`() = runTest {
        val videos = listOf(
            VideoDto(key = "vimeo", site = "Vimeo", type = "Trailer", official = true)
        )
        coEvery { apiService.getMovieVideos(1) } returns VideoResponse(id = 1, results = videos)

        val result = repository.getMovieTrailerKey(1)

        assertNull(result)
    }

    @Test
    fun `getMovieTrailerKey returns null when no videos`() = runTest {
        coEvery { apiService.getMovieVideos(1) } returns VideoResponse(id = 1, results = emptyList())

        val result = repository.getMovieTrailerKey(1)

        assertNull(result)
    }

    // --- getMovieCertification ---

    @Test
    fun `getMovieCertification returns KR certification when available`() = runTest {
        val response = ReleaseDateResponse(
            id = 1,
            results = listOf(
                ReleaseDateResult("KR", listOf(ReleaseDateInfo(certification = "15"))),
                ReleaseDateResult("US", listOf(ReleaseDateInfo(certification = "PG-13")))
            )
        )
        coEvery { apiService.getMovieReleaseDates(1) } returns response

        val result = repository.getMovieCertification(1)

        assertEquals("15", result)
    }

    @Test
    fun `getMovieCertification falls back to US when KR absent`() = runTest {
        val response = ReleaseDateResponse(
            id = 1,
            results = listOf(
                ReleaseDateResult("US", listOf(ReleaseDateInfo(certification = "R")))
            )
        )
        coEvery { apiService.getMovieReleaseDates(1) } returns response

        val result = repository.getMovieCertification(1)

        assertEquals("R", result)
    }

    @Test
    fun `getMovieCertification returns null when no results`() = runTest {
        val response = ReleaseDateResponse(id = 1, results = emptyList())
        coEvery { apiService.getMovieReleaseDates(1) } returns response

        val result = repository.getMovieCertification(1)

        assertNull(result)
    }

    @Test
    fun `getMovieCertification returns null when certifications are blank`() = runTest {
        val response = ReleaseDateResponse(
            id = 1,
            results = listOf(
                ReleaseDateResult(
                    "KR",
                    listOf(
                        ReleaseDateInfo(certification = ""),
                        ReleaseDateInfo(certification = "")
                    )
                )
            )
        )
        coEvery { apiService.getMovieReleaseDates(1) } returns response

        val result = repository.getMovieCertification(1)

        assertNull(result)
    }

    // --- getGenreList ---

    @Test
    fun `getGenreList returns mapped genre list`() = runTest {
        val response = GenreListResponse(
            genres = listOf(GenreDto(28, "Action"), GenreDto(35, "Comedy"))
        )
        coEvery { apiService.getGenreList() } returns response

        val result = repository.getGenreList()

        assertEquals(2, result.size)
        assertEquals(28, result[0].id)
        assertEquals("Action", result[0].name)
        assertEquals(35, result[1].id)
        assertEquals("Comedy", result[1].name)
    }

    // --- getMovieReviews ---

    @Test
    fun `getMovieReviews returns mapped domain reviews`() = runTest {
        val response = ReviewResponse(
            results = listOf(
                ReviewDto(
                    id = "r1",
                    author = "Reviewer",
                    authorDetails = AuthorDetailsDto(avatarPath = "/avatar.jpg", rating = 8.0),
                    content = "Great movie!",
                    createdAt = "2024-01-01T00:00:00.000Z"
                )
            )
        )
        coEvery { apiService.getMovieReviews(1) } returns response

        val reviews = repository.getMovieReviews(1)

        assertEquals(1, reviews.size)
        assertEquals("Reviewer", reviews[0].author)
        assertEquals(8.0, reviews[0].rating)
        assertEquals("Great movie!", reviews[0].content)
    }

    @Test
    fun `getMovieReviews with invalid movieId throws exception`() = runTest {
        try {
            repository.getMovieReviews(0)
            assertTrue("Should throw", false)
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    // --- getMovieRecommendations ---

    @Test
    fun `getMovieRecommendations returns mapped movies`() = runTest {
        coEvery { apiService.getMovieRecommendations(1) } returns MovieListResponse(
            page = 1, results = testMovieDtos, totalPages = 1, totalResults = 2
        )

        val result = repository.getMovieRecommendations(1)

        assertEquals(2, result.size)
        assertEquals("Similar 1", result[0].title)
        assertEquals(10, result[0].id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getMovieRecommendations throws on invalid movieId`() = runTest {
        repository.getMovieRecommendations(0)
    }

    // --- paging methods ---

    @Test(expected = IllegalArgumentException::class)
    fun `searchMovies with blank query throws IllegalArgumentException`() = runTest {
        repository.searchMovies("")
    }

    @Test
    fun `searchMovies with valid query returns non-null PagingData flow`() {
        val flow: Flow<PagingData<Movie>> = repository.searchMovies("action")

        assertNotNull(flow)
    }

    @Test
    fun `discoverMovies with empty genres returns non-null PagingData flow`() {
        val flow: Flow<PagingData<Movie>> = repository.discoverMovies(
            genres = emptySet(),
            sortBy = SortOption.POPULARITY_DESC.apiValue
        )

        assertNotNull(flow)
    }

    @Test
    fun `getTrendingMovies returns non-null PagingData flow`() {
        val flow: Flow<PagingData<Movie>> = repository.getTrendingMovies()

        assertNotNull(flow)
    }

    @Test
    fun `getNowPlayingMovies returns non-null PagingData flow`() {
        every { cachedMovieDao.getMoviesByCategory(any()) } returns mockk()

        val flow: Flow<PagingData<Movie>> = repository.getNowPlayingMovies()

        assertNotNull(flow)
    }

    @Test
    fun `getPopularMovies returns non-null PagingData flow`() {
        every { cachedMovieDao.getMoviesByCategory(any()) } returns mockk()

        val flow: Flow<PagingData<Movie>> = repository.getPopularMovies()

        assertNotNull(flow)
    }
}
