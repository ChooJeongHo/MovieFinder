package com.choo.moviefinder.data.paging

import androidx.paging.PagingSource
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.MovieDto
import com.choo.moviefinder.data.remote.dto.MovieListResponse
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

class MoviePagingSourceTest {

    private lateinit var apiService: MovieApiService

    private val testMovieDto = MovieDto(
        id = 1,
        title = "Test Movie",
        posterPath = "/poster.jpg",
        backdropPath = "/backdrop.jpg",
        overview = "Test overview",
        releaseDate = "2024-01-01",
        voteAverage = 8.0,
        voteCount = 100
    )

    @Before
    fun setup() {
        apiService = mockk()
    }

    private fun createResponse(page: Int, totalPages: Int) = MovieListResponse(
        page = page,
        results = listOf(testMovieDto),
        totalPages = totalPages,
        totalResults = 100
    )

    @Test
    fun `refresh loads first page successfully`() = runTest {
        coEvery { apiService.searchMovies("test", 1, any(), year = null) } returns
            createResponse(1, 5)

        val pagingSource = MoviePagingSource(apiService, "test")
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(1, page.data.size)
        assertEquals("Test Movie", page.data[0].title)
        assertNull(page.prevKey)
        assertEquals(2, page.nextKey)
    }

    @Test
    fun `refresh returns error on exception`() = runTest {
        coEvery { apiService.searchMovies("test", 1, any(), year = null) } throws
            IOException("Network error")

        val pagingSource = MoviePagingSource(apiService, "test")
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
        )

        assertTrue(result is PagingSource.LoadResult.Error)
        assertTrue((result as PagingSource.LoadResult.Error).throwable is IOException)
    }

    @Test
    fun `append loads next page`() = runTest {
        coEvery { apiService.searchMovies("test", 2, any(), year = null) } returns
            createResponse(2, 5)

        val pagingSource = MoviePagingSource(apiService, "test")
        val result = pagingSource.load(
            PagingSource.LoadParams.Append(key = 2, loadSize = 20, placeholdersEnabled = false)
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(1, page.prevKey)
        assertEquals(3, page.nextKey)
    }

    @Test
    fun `last page returns null nextKey`() = runTest {
        coEvery { apiService.searchMovies("test", 5, any(), year = null) } returns
            createResponse(5, 5)

        val pagingSource = MoviePagingSource(apiService, "test")
        val result = pagingSource.load(
            PagingSource.LoadParams.Append(key = 5, loadSize = 20, placeholdersEnabled = false)
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        assertNull((result as PagingSource.LoadResult.Page).nextKey)
    }

    @Test
    fun `first page returns null prevKey`() = runTest {
        coEvery { apiService.searchMovies("test", 1, any(), year = null) } returns
            createResponse(1, 5)

        val pagingSource = MoviePagingSource(apiService, "test")
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        assertNull((result as PagingSource.LoadResult.Page).prevKey)
    }

    @Test
    fun `year parameter is passed to API`() = runTest {
        coEvery { apiService.searchMovies("test", 1, any(), year = 2024) } returns
            createResponse(1, 1)

        val pagingSource = MoviePagingSource(apiService, "test", year = 2024)
        pagingSource.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
        )

        coVerify { apiService.searchMovies("test", 1, any(), year = 2024) }
    }

    @Test
    fun `getRefreshKey returns null when no anchor`() {
        val pagingSource = MoviePagingSource(apiService, "test")
        val state = PagingSource.LoadResult.Page<Int, com.choo.moviefinder.domain.model.Movie>(
            data = emptyList(),
            prevKey = null,
            nextKey = null
        )
        val pagingState = androidx.paging.PagingState(
            pages = listOf(state),
            anchorPosition = null,
            config = androidx.paging.PagingConfig(pageSize = 20),
            leadingPlaceholderCount = 0
        )
        assertNull(pagingSource.getRefreshKey(pagingState))
    }
}
