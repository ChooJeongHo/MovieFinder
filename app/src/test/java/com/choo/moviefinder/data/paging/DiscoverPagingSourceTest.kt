package com.choo.moviefinder.data.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.MovieDto
import com.choo.moviefinder.data.remote.dto.MovieListResponse
import com.choo.moviefinder.domain.model.Movie
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class DiscoverPagingSourceTest {

    private lateinit var apiService: MovieApiService

    private val testMovieDto = MovieDto(
        id = 1,
        title = "Discover Movie",
        posterPath = "/poster.jpg",
        backdropPath = "/backdrop.jpg",
        overview = "Discover overview",
        releaseDate = "2024-01-01",
        voteAverage = 7.5,
        voteCount = 200
    )

    @Before
    fun setup() {
        apiService = mockk()
    }

    private fun createResponse(page: Int, totalPages: Int) = MovieListResponse(
        page = page,
        results = listOf(testMovieDto),
        totalPages = totalPages,
        totalResults = 50
    )

    @Test
    fun `refresh loads first page successfully`() = runTest {
        coEvery {
            apiService.discoverMovies(1, "28", "popularity.desc", null, any())
        } returns createResponse(1, 3)

        val source = DiscoverPagingSource(apiService, "28", "popularity.desc", null)
        val result = source.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(1, page.data.size)
        assertEquals("Discover Movie", page.data[0].title)
        assertNull(page.prevKey)
        assertEquals(2, page.nextKey)
    }

    @Test
    fun `refresh returns error on exception`() = runTest {
        coEvery {
            apiService.discoverMovies(1, any(), any(), any(), any())
        } throws IOException("Network error")

        val source = DiscoverPagingSource(apiService, null, "popularity.desc", null)
        val result = source.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
        )

        assertTrue(result is PagingSource.LoadResult.Error)
    }

    @Test
    fun `parameters are passed correctly to API`() = runTest {
        coEvery {
            apiService.discoverMovies(1, "28,12", "vote_average.desc", 2024, any())
        } returns createResponse(1, 1)

        val source = DiscoverPagingSource(apiService, "28,12", "vote_average.desc", 2024)
        source.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
        )

        coVerify {
            apiService.discoverMovies(1, "28,12", "vote_average.desc", 2024, any())
        }
    }

    @Test
    fun `last page returns null nextKey`() = runTest {
        coEvery {
            apiService.discoverMovies(3, any(), any(), any(), any())
        } returns createResponse(3, 3)

        val source = DiscoverPagingSource(apiService, null, "popularity.desc", null)
        val result = source.load(
            PagingSource.LoadParams.Append(key = 3, loadSize = 20, placeholdersEnabled = false)
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        assertNull((result as PagingSource.LoadResult.Page).nextKey)
    }

    @Test
    fun `append loads next page with correct keys`() = runTest {
        coEvery {
            apiService.discoverMovies(2, any(), any(), any(), any())
        } returns createResponse(2, 5)

        val source = DiscoverPagingSource(apiService, null, "popularity.desc", null)
        val result = source.load(
            PagingSource.LoadParams.Append(key = 2, loadSize = 20, placeholdersEnabled = false)
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(1, page.prevKey)
        assertEquals(3, page.nextKey)
    }

    // ── getRefreshKey ───────────────────────────────────────────

    private val testMovie = Movie(1, "Movie", null, null, "", "2024-01-01", 7.0, 100)

    private fun pagingState(
        pages: List<PagingSource.LoadResult.Page<Int, Movie>>,
        anchorPosition: Int?
    ) = PagingState(
        pages = pages,
        anchorPosition = anchorPosition,
        config = PagingConfig(pageSize = 20),
        leadingPlaceholderCount = 0
    )

    @Test
    fun `getRefreshKey returns null when anchorPosition is null`() {
        val source = DiscoverPagingSource(apiService, null, "popularity.desc", null)
        val state = pagingState(emptyList(), anchorPosition = null)
        assertNull(source.getRefreshKey(state))
    }

    @Test
    fun `getRefreshKey returns prevKey plus 1 when anchor page has prevKey`() {
        val source = DiscoverPagingSource(apiService, null, "popularity.desc", null)
        val page = PagingSource.LoadResult.Page<Int, Movie>(
            data = listOf(testMovie), prevKey = 1, nextKey = 3
        )
        val state = pagingState(listOf(page), anchorPosition = 0)
        assertEquals(2, source.getRefreshKey(state))
    }

    @Test
    fun `getRefreshKey falls back to nextKey minus 1 when prevKey is null`() {
        val source = DiscoverPagingSource(apiService, null, "popularity.desc", null)
        val page = PagingSource.LoadResult.Page<Int, Movie>(
            data = listOf(testMovie), prevKey = null, nextKey = 2
        )
        val state = pagingState(listOf(page), anchorPosition = 0)
        assertEquals(1, source.getRefreshKey(state))
    }
}
