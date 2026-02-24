package com.choo.moviefinder.data.repository

import app.cash.turbine.test
import com.choo.moviefinder.data.local.MovieDatabase
import com.choo.moviefinder.data.local.dao.CachedMovieDao
import com.choo.moviefinder.data.local.dao.FavoriteMovieDao
import com.choo.moviefinder.data.local.dao.RecentSearchDao
import com.choo.moviefinder.data.local.dao.RemoteKeyDao
import com.choo.moviefinder.data.local.entity.FavoriteMovieEntity
import com.choo.moviefinder.data.local.entity.RecentSearchEntity
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.CastDto
import com.choo.moviefinder.data.remote.dto.CreditsResponse
import com.choo.moviefinder.data.remote.dto.GenreDto
import com.choo.moviefinder.data.remote.dto.MovieDetailDto
import com.choo.moviefinder.data.remote.dto.MovieDto
import com.choo.moviefinder.data.remote.dto.MovieListResponse
import com.choo.moviefinder.data.remote.dto.VideoDto
import com.choo.moviefinder.data.remote.dto.VideoResponse
import com.choo.moviefinder.domain.model.Movie
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieRepositoryImplTest {

    private lateinit var apiService: MovieApiService
    private lateinit var database: MovieDatabase
    private lateinit var favoriteMovieDao: FavoriteMovieDao
    private lateinit var recentSearchDao: RecentSearchDao
    private lateinit var cachedMovieDao: CachedMovieDao
    private lateinit var remoteKeyDao: RemoteKeyDao

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

    private val testFavoriteEntities = listOf(
        FavoriteMovieEntity(
            id = 1, title = "Fav 1", posterPath = "/f1.jpg",
            backdropPath = "/fb1.jpg", overview = "Fav overview 1",
            releaseDate = "2024-01-01", voteAverage = 8.0, voteCount = 100
        ),
        FavoriteMovieEntity(
            id = 2, title = "Fav 2", posterPath = "/f2.jpg",
            backdropPath = "/fb2.jpg", overview = "Fav overview 2",
            releaseDate = "2024-02-01", voteAverage = 7.5, voteCount = 200
        )
    )

    @Before
    fun setup() {
        apiService = mockk()
        database = mockk()
        favoriteMovieDao = mockk(relaxUnitFun = true)
        recentSearchDao = mockk(relaxUnitFun = true)
        cachedMovieDao = mockk()
        remoteKeyDao = mockk()

        repository = MovieRepositoryImpl(
            apiService = apiService,
            database = database,
            favoriteMovieDao = favoriteMovieDao,
            recentSearchDao = recentSearchDao,
            cachedMovieDao = cachedMovieDao,
            remoteKeyDao = remoteKeyDao
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

    // --- toggleFavorite ---

    @Test
    fun `toggleFavorite delegates to dao toggleFavorite`() = runTest {
        val movie = Movie(1, "Test", "/p.jpg", "/b.jpg", "Overview", "2024-01-01", 8.0, 100)

        repository.toggleFavorite(movie)

        coVerify { favoriteMovieDao.toggleFavorite(match { it.id == 1 && it.title == "Test" }) }
    }

    // --- isFavorite ---

    @Test
    fun `isFavorite returns flow from dao`() = runTest {
        every { favoriteMovieDao.isFavorite(1) } returns flowOf(true)

        repository.isFavorite(1).test {
            assertTrue(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `isFavorite returns false for non-favorite`() = runTest {
        every { favoriteMovieDao.isFavorite(99) } returns flowOf(false)

        repository.isFavorite(99).test {
            assertFalse(awaitItem())
            awaitComplete()
        }
    }

    // --- getFavoriteMovies ---

    @Test
    fun `getFavoriteMovies returns mapped domain movies`() = runTest {
        every { favoriteMovieDao.getAllFavorites() } returns flowOf(testFavoriteEntities)

        repository.getFavoriteMovies().test {
            val movies = awaitItem()
            assertEquals(2, movies.size)
            assertEquals("Fav 1", movies[0].title)
            assertEquals(1, movies[0].id)
            assertEquals("Fav 2", movies[1].title)
            assertEquals(2, movies[1].id)
            awaitComplete()
        }
    }

    @Test
    fun `getFavoriteMovies returns empty list when no favorites`() = runTest {
        every { favoriteMovieDao.getAllFavorites() } returns flowOf(emptyList())

        repository.getFavoriteMovies().test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    // --- getRecentSearches ---

    @Test
    fun `getRecentSearches returns query strings`() = runTest {
        val entities = listOf(
            RecentSearchEntity(query = "Batman"),
            RecentSearchEntity(query = "Spider-Man")
        )
        every { recentSearchDao.getRecentSearches() } returns flowOf(entities)

        repository.getRecentSearches().test {
            val searches = awaitItem()
            assertEquals(2, searches.size)
            assertEquals("Batman", searches[0])
            assertEquals("Spider-Man", searches[1])
            awaitComplete()
        }
    }

    // --- saveSearchQuery ---

    @Test
    fun `saveSearchQuery inserts entity into dao`() = runTest {
        repository.saveSearchQuery("Avengers")

        coVerify { recentSearchDao.insert(match { it.query == "Avengers" }) }
    }

    // --- deleteSearchQuery ---

    @Test
    fun `deleteSearchQuery calls dao delete`() = runTest {
        repository.deleteSearchQuery("Batman")

        coVerify { recentSearchDao.delete("Batman") }
    }

    // --- clearSearchHistory ---

    @Test
    fun `clearSearchHistory calls dao clearAll`() = runTest {
        repository.clearSearchHistory()

        coVerify { recentSearchDao.clearAll() }
    }
}
