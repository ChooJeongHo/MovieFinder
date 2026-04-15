package com.choo.moviefinder.data.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.MovieDto
import com.choo.moviefinder.data.remote.dto.MovieListResponse
import com.choo.moviefinder.domain.model.Movie
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class TrendingPagingSourceTest {

    private lateinit var apiService: MovieApiService

    private val testMovieDto = MovieDto(
        id = 1,
        title = "Trending Movie",
        posterPath = "/poster.jpg",
        backdropPath = "/backdrop.jpg",
        overview = "Test overview",
        releaseDate = "2024-01-01",
        voteAverage = 7.5,
        voteCount = 200
    )

    @Before
    fun setup() {
        apiService = mockk()
    }

    private fun createResponse(resultCount: Int = 20) = MovieListResponse(
        page = 1,
        results = List(resultCount) { testMovieDto.copy(id = it + 1) },
        totalPages = 5,
        totalResults = 100
    )

    @Test
    fun `load returns first page successfully`() = runTest {
        coEvery { apiService.getTrendingMovies(1) } returns createResponse()

        val pagingSource = TrendingPagingSource(apiService)
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(20, page.data.size)
        assertEquals("Trending Movie", page.data[0].title)
        assertNull(page.prevKey)
        assertEquals(2, page.nextKey)
    }

    @Test
    fun `load returns error on exception`() = runTest {
        coEvery { apiService.getTrendingMovies(1) } throws IOException("Network error")

        val pagingSource = TrendingPagingSource(apiService)
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
        )

        assertTrue(result is PagingSource.LoadResult.Error)
        assertTrue((result as PagingSource.LoadResult.Error).throwable is IOException)
    }

    @Test
    fun `load returns next page`() = runTest {
        coEvery { apiService.getTrendingMovies(2) } returns createResponse()

        val pagingSource = TrendingPagingSource(apiService)
        val result = pagingSource.load(
            PagingSource.LoadParams.Append(key = 2, loadSize = 20, placeholdersEnabled = false)
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(1, page.prevKey)
        assertEquals(3, page.nextKey)
    }

    @Test
    fun `last page has null nextKey`() = runTest {
        // totalPages = 3이고 현재 page = 3 → page >= totalPages → nextKey null
        val lastPageResponse = MovieListResponse(
            page = 3, results = List(10) { testMovieDto.copy(id = it + 1) },
            totalPages = 3, totalResults = 50
        )
        coEvery { apiService.getTrendingMovies(3) } returns lastPageResponse

        val pagingSource = TrendingPagingSource(apiService)
        val result = pagingSource.load(
            PagingSource.LoadParams.Append(key = 3, loadSize = 20, placeholdersEnabled = false)
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        assertNull((result as PagingSource.LoadResult.Page).nextKey)
    }

    @Test
    fun `first page has null prevKey`() = runTest {
        coEvery { apiService.getTrendingMovies(1) } returns createResponse()

        val pagingSource = TrendingPagingSource(apiService)
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        assertNull((result as PagingSource.LoadResult.Page).prevKey)
    }

    // ── getRefreshKey ───────────────────────────────────────────

    private val testDomainMovie = Movie(1, "Trending", null, null, "", "2024-01-01", 7.0, 100)

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
        val pagingSource = TrendingPagingSource(apiService)
        val state = pagingState(emptyList(), anchorPosition = null)
        assertNull(pagingSource.getRefreshKey(state))
    }

    @Test
    fun `getRefreshKey returns prevKey plus 1 when anchor page has prevKey`() {
        val pagingSource = TrendingPagingSource(apiService)
        val page = PagingSource.LoadResult.Page<Int, Movie>(
            data = listOf(testDomainMovie), prevKey = 1, nextKey = 3
        )
        val state = pagingState(listOf(page), anchorPosition = 0)
        assertEquals(2, pagingSource.getRefreshKey(state))
    }

    @Test
    fun `getRefreshKey falls back to nextKey minus 1 when prevKey is null`() {
        val pagingSource = TrendingPagingSource(apiService)
        val page = PagingSource.LoadResult.Page<Int, Movie>(
            data = listOf(testDomainMovie), prevKey = null, nextKey = 2
        )
        val state = pagingState(listOf(page), anchorPosition = 0)
        assertEquals(1, pagingSource.getRefreshKey(state))
    }
}
