package com.choo.moviefinder.data.paging

import androidx.paging.PagingSource
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.MovieDto
import com.choo.moviefinder.data.remote.dto.MovieListResponse
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
        coEvery { apiService.getTrendingMovies(3) } returns createResponse(resultCount = 5)

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
}
