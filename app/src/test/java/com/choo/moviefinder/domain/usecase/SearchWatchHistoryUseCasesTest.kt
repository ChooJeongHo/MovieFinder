package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.SearchHistoryRepository
import com.choo.moviefinder.domain.repository.WatchHistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SearchWatchHistoryUseCasesTest {

    private lateinit var searchHistoryRepository: SearchHistoryRepository
    private lateinit var watchHistoryRepository: WatchHistoryRepository

    private val testMovie = Movie(
        id = 10,
        title = "History Movie",
        posterPath = "/poster.jpg",
        backdropPath = null,
        overview = "Overview",
        releaseDate = "2024-06-01",
        voteAverage = 8.0,
        voteCount = 500
    )

    @Before
    fun setUp() {
        searchHistoryRepository = mockk()
        watchHistoryRepository = mockk()
    }

    // --- GetRecentSearchesUseCase ---

    @Test
    fun `GetRecentSearchesUseCase delegates to repository`() {
        val flow = flowOf(listOf("avengers", "batman"))
        every { searchHistoryRepository.getRecentSearches() } returns flow
        val useCase = GetRecentSearchesUseCase(searchHistoryRepository)

        val result = useCase()

        verify(exactly = 1) { searchHistoryRepository.getRecentSearches() }
        assertEquals(flow, result)
    }

    @Test
    fun `GetRecentSearchesUseCase returns list from repository`() = runTest {
        every { searchHistoryRepository.getRecentSearches() } returns flowOf(listOf("iron man"))
        val useCase = GetRecentSearchesUseCase(searchHistoryRepository)

        val result = useCase().first()

        assertEquals(listOf("iron man"), result)
    }

    // --- SaveSearchQueryUseCase ---

    @Test
    fun `SaveSearchQueryUseCase calls saveSearchQuery on repository`() = runTest {
        coEvery { searchHistoryRepository.saveSearchQuery("thor") } returns Unit
        val useCase = SaveSearchQueryUseCase(searchHistoryRepository)

        useCase("thor")

        coVerify(exactly = 1) { searchHistoryRepository.saveSearchQuery("thor") }
    }

    @Test
    fun `SaveSearchQueryUseCase passes correct query string`() = runTest {
        val captured = mutableListOf<String>()
        coEvery { searchHistoryRepository.saveSearchQuery(capture(captured)) } returns Unit
        val useCase = SaveSearchQueryUseCase(searchHistoryRepository)

        useCase("spider-man")

        assertEquals("spider-man", captured.first())
    }

    // --- DeleteSearchQueryUseCase ---

    @Test
    fun `DeleteSearchQueryUseCase calls deleteSearchQuery on repository`() = runTest {
        coEvery { searchHistoryRepository.deleteSearchQuery("old query") } returns Unit
        val useCase = DeleteSearchQueryUseCase(searchHistoryRepository)

        useCase("old query")

        coVerify(exactly = 1) { searchHistoryRepository.deleteSearchQuery("old query") }
    }

    @Test
    fun `DeleteSearchQueryUseCase passes correct query to repository`() = runTest {
        val captured = mutableListOf<String>()
        coEvery { searchHistoryRepository.deleteSearchQuery(capture(captured)) } returns Unit
        val useCase = DeleteSearchQueryUseCase(searchHistoryRepository)

        useCase("delete me")

        assertEquals("delete me", captured.first())
    }

    // --- ClearSearchHistoryUseCase ---

    @Test
    fun `ClearSearchHistoryUseCase calls clearSearchHistory on repository`() = runTest {
        coEvery { searchHistoryRepository.clearSearchHistory() } returns Unit
        val useCase = ClearSearchHistoryUseCase(searchHistoryRepository)

        useCase()

        coVerify(exactly = 1) { searchHistoryRepository.clearSearchHistory() }
    }

    @Test
    fun `ClearSearchHistoryUseCase invokes repository only once`() = runTest {
        coEvery { searchHistoryRepository.clearSearchHistory() } returns Unit
        val useCase = ClearSearchHistoryUseCase(searchHistoryRepository)

        useCase()

        coVerify(exactly = 1) { searchHistoryRepository.clearSearchHistory() }
    }

    // --- GetWatchHistoryUseCase ---

    @Test
    fun `GetWatchHistoryUseCase delegates to repository`() {
        val flow = flowOf(listOf(testMovie))
        every { watchHistoryRepository.getWatchHistory() } returns flow
        val useCase = GetWatchHistoryUseCase(watchHistoryRepository)

        val result = useCase()

        verify(exactly = 1) { watchHistoryRepository.getWatchHistory() }
        assertEquals(flow, result)
    }

    @Test
    fun `GetWatchHistoryUseCase returns list from repository`() = runTest {
        every { watchHistoryRepository.getWatchHistory() } returns flowOf(listOf(testMovie))
        val useCase = GetWatchHistoryUseCase(watchHistoryRepository)

        val result = useCase().first()

        assertEquals(listOf(testMovie), result)
    }

    // --- SaveWatchHistoryUseCase (branches) ---

    @Test
    fun `SaveWatchHistoryUseCase calls saveWatchHistory when genres is empty`() = runTest {
        coEvery { watchHistoryRepository.saveWatchHistory(testMovie) } returns Unit
        val useCase = SaveWatchHistoryUseCase(watchHistoryRepository)

        useCase(testMovie, "")

        coVerify(exactly = 1) { watchHistoryRepository.saveWatchHistory(testMovie) }
        coVerify(exactly = 0) { watchHistoryRepository.saveWatchHistoryWithGenres(any(), any()) }
    }

    @Test
    fun `SaveWatchHistoryUseCase calls saveWatchHistoryWithGenres when genres is not empty`() = runTest {
        coEvery { watchHistoryRepository.saveWatchHistoryWithGenres(testMovie, "Action,Drama") } returns Unit
        val useCase = SaveWatchHistoryUseCase(watchHistoryRepository)

        useCase(testMovie, "Action,Drama")

        coVerify(exactly = 1) { watchHistoryRepository.saveWatchHistoryWithGenres(testMovie, "Action,Drama") }
        coVerify(exactly = 0) { watchHistoryRepository.saveWatchHistory(any()) }
    }

    @Test
    fun `SaveWatchHistoryUseCase uses saveWatchHistory by default when genres omitted`() = runTest {
        coEvery { watchHistoryRepository.saveWatchHistory(testMovie) } returns Unit
        val useCase = SaveWatchHistoryUseCase(watchHistoryRepository)

        useCase(testMovie)

        coVerify(exactly = 1) { watchHistoryRepository.saveWatchHistory(testMovie) }
    }

    // --- ClearWatchHistoryUseCase ---

    @Test
    fun `ClearWatchHistoryUseCase calls clearWatchHistory on repository`() = runTest {
        coEvery { watchHistoryRepository.clearWatchHistory() } returns Unit
        val useCase = ClearWatchHistoryUseCase(watchHistoryRepository)

        useCase()

        coVerify(exactly = 1) { watchHistoryRepository.clearWatchHistory() }
    }

    @Test
    fun `ClearWatchHistoryUseCase invokes repository once`() = runTest {
        coEvery { watchHistoryRepository.clearWatchHistory() } returns Unit
        val useCase = ClearWatchHistoryUseCase(watchHistoryRepository)

        useCase()

        coVerify(exactly = 1) { watchHistoryRepository.clearWatchHistory() }
    }
}
