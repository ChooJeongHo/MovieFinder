package com.choo.moviefinder.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.choo.moviefinder.data.local.MovieDatabase
import com.choo.moviefinder.data.local.dao.CachedMovieDao
import com.choo.moviefinder.data.local.dao.RemoteKeyDao
import com.choo.moviefinder.data.local.entity.CachedMovieEntity
import com.choo.moviefinder.data.local.entity.RemoteKeyEntity
import com.choo.moviefinder.core.util.NetworkMonitor
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.MovieDto
import com.choo.moviefinder.data.remote.dto.MovieListResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class MovieRemoteMediatorTest {

    private lateinit var apiService: MovieApiService
    private lateinit var database: MovieDatabase
    private lateinit var cachedMovieDao: CachedMovieDao
    private lateinit var remoteKeyDao: RemoteKeyDao
    private lateinit var networkMonitor: NetworkMonitor

    private val testMovieDto = MovieDto(
        id = 1,
        title = "Test Movie",
        posterPath = "/poster.jpg",
        backdropPath = "/backdrop.jpg",
        overview = "Overview",
        releaseDate = "2024-01-01",
        voteAverage = 8.0,
        voteCount = 100
    )

    @Before
    fun setup() {
        apiService = mockk()
        database = mockk(relaxed = true)
        cachedMovieDao = mockk(relaxed = true)
        remoteKeyDao = mockk(relaxed = true)
        networkMonitor = mockk {
            every { isConnected } returns MutableStateFlow(true)
        }
    }

    private fun createMediator(category: String = MovieRemoteMediator.CATEGORY_NOW_PLAYING) =
        MovieRemoteMediator(apiService, database, cachedMovieDao, remoteKeyDao, category, networkMonitor)

    private fun createEmptyPagingState() = PagingState<Int, CachedMovieEntity>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = 20),
        leadingPlaceholderCount = 0
    )

    private fun createResponse(page: Int, totalPages: Int) = MovieListResponse(
        page = page,
        results = listOf(testMovieDto),
        totalPages = totalPages,
        totalResults = 100
    )

    @Test
    fun `initialize returns LAUNCH_INITIAL_REFRESH when no cache`() = runTest {
        coEvery { remoteKeyDao.getRemoteKey(any()) } returns null

        val mediator = createMediator()
        val result = mediator.initialize()

        assertTrue(result == RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH)
    }

    @Test
    fun `initialize returns LAUNCH_INITIAL_REFRESH when cache expired`() = runTest {
        val expiredKey = RemoteKeyEntity(
            category = MovieRemoteMediator.CATEGORY_NOW_PLAYING,
            nextKey = 2,
            lastUpdated = System.currentTimeMillis() - MovieRemoteMediator.CACHE_TIMEOUT_MS - 1
        )
        coEvery { remoteKeyDao.getRemoteKey(any()) } returns expiredKey

        val mediator = createMediator()
        val result = mediator.initialize()

        assertTrue(result == RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH)
    }

    @Test
    fun `initialize returns SKIP_INITIAL_REFRESH when cache fresh`() = runTest {
        val freshKey = RemoteKeyEntity(
            category = MovieRemoteMediator.CATEGORY_NOW_PLAYING,
            nextKey = 2,
            lastUpdated = System.currentTimeMillis()
        )
        coEvery { remoteKeyDao.getRemoteKey(any()) } returns freshKey

        val mediator = createMediator()
        val result = mediator.initialize()

        assertTrue(result == RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH)
    }

    @Test
    fun `load PREPEND returns success with endOfPagination true`() = runTest {
        val mediator = createMediator()
        val result = mediator.load(LoadType.PREPEND, createEmptyPagingState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
    }

    @Test
    fun `load APPEND with no remote key returns endOfPagination`() = runTest {
        coEvery { remoteKeyDao.getRemoteKey(any()) } returns null

        val mediator = createMediator()
        val result = mediator.load(LoadType.APPEND, createEmptyPagingState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
    }

    @Test
    fun `load APPEND with no nextKey returns endOfPagination`() = runTest {
        val keyWithNoNext = RemoteKeyEntity(
            category = MovieRemoteMediator.CATEGORY_NOW_PLAYING,
            nextKey = null
        )
        coEvery { remoteKeyDao.getRemoteKey(any()) } returns keyWithNoNext

        val mediator = createMediator()
        val result = mediator.load(LoadType.APPEND, createEmptyPagingState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
    }

    @Test
    fun `load returns error on API exception`() = runTest {
        coEvery { apiService.getNowPlayingMovies(1, any()) } throws IOException("Network error")

        val mediator = createMediator()
        val result = mediator.load(LoadType.REFRESH, createEmptyPagingState())

        assertTrue(result is RemoteMediator.MediatorResult.Error)
        assertTrue((result as RemoteMediator.MediatorResult.Error).throwable is IOException)
    }

    @Test
    fun `load REFRESH when offline returns success without calling API`() = runTest {
        val offlineMonitor = mockk<NetworkMonitor> {
            every { isConnected } returns MutableStateFlow(false)
        }
        val mediator = MovieRemoteMediator(
            apiService, database, cachedMovieDao, remoteKeyDao,
            MovieRemoteMediator.CATEGORY_NOW_PLAYING, offlineMonitor
        )

        val result = mediator.load(LoadType.REFRESH, createEmptyPagingState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertFalse((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        coVerify(exactly = 0) { apiService.getNowPlayingMovies(any(), any()) }
    }
}
