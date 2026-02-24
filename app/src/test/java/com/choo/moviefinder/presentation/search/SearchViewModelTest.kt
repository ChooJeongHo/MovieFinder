package com.choo.moviefinder.presentation.search

import app.cash.turbine.test
import com.choo.moviefinder.domain.usecase.ClearSearchHistoryUseCase
import com.choo.moviefinder.domain.usecase.DeleteSearchQueryUseCase
import com.choo.moviefinder.domain.usecase.GetRecentSearchesUseCase
import com.choo.moviefinder.domain.usecase.SaveSearchQueryUseCase
import com.choo.moviefinder.domain.usecase.SearchMoviesUseCase
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var searchMoviesUseCase: SearchMoviesUseCase
    private lateinit var getRecentSearchesUseCase: GetRecentSearchesUseCase
    private lateinit var saveSearchQueryUseCase: SaveSearchQueryUseCase
    private lateinit var deleteSearchQueryUseCase: DeleteSearchQueryUseCase
    private lateinit var clearSearchHistoryUseCase: ClearSearchHistoryUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        searchMoviesUseCase = mockk()
        getRecentSearchesUseCase = mockk()
        saveSearchQueryUseCase = mockk()
        deleteSearchQueryUseCase = mockk()
        clearSearchHistoryUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        recentSearches: List<String> = emptyList(),
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ): SearchViewModel {
        every { getRecentSearchesUseCase() } returns flowOf(recentSearches)
        return SearchViewModel(
            savedStateHandle = savedStateHandle,
            searchMoviesUseCase = searchMoviesUseCase,
            getRecentSearchesUseCase = getRecentSearchesUseCase,
            saveSearchQueryUseCase = saveSearchQueryUseCase,
            deleteSearchQueryUseCase = deleteSearchQueryUseCase,
            clearSearchHistoryUseCase = clearSearchHistoryUseCase
        )
    }

    @Test
    fun `initial searchQuery is empty`() = runTest {
        val viewModel = createViewModel()
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `onSearchQueryChange updates searchQuery`() = runTest {
        val viewModel = createViewModel()

        viewModel.onSearchQueryChange("test")

        assertEquals("test", viewModel.searchQuery.value)
    }

    @Test
    fun `onSearch saves query via use case`() = runTest {
        coEvery { saveSearchQueryUseCase(any()) } returns Unit
        val viewModel = createViewModel()

        viewModel.onSearch("avengers")
        advanceUntilIdle()

        coVerify { saveSearchQueryUseCase("avengers") }
    }

    @Test
    fun `onSearch with blank query does not save`() = runTest {
        val viewModel = createViewModel()

        viewModel.onSearch("   ")
        advanceUntilIdle()

        coVerify(exactly = 0) { saveSearchQueryUseCase(any()) }
    }

    @Test
    fun `onDeleteRecentSearch calls delete use case`() = runTest {
        coEvery { deleteSearchQueryUseCase(any()) } returns Unit
        val viewModel = createViewModel()

        viewModel.onDeleteRecentSearch("old query")
        advanceUntilIdle()

        coVerify { deleteSearchQueryUseCase("old query") }
    }

    @Test
    fun `onClearSearchHistory calls clear use case`() = runTest {
        coEvery { clearSearchHistoryUseCase() } returns Unit
        val viewModel = createViewModel()

        viewModel.onClearSearchHistory()
        advanceUntilIdle()

        coVerify { clearSearchHistoryUseCase() }
    }

    @Test
    fun `onYearSelected updates selectedYear`() = runTest {
        val viewModel = createViewModel()

        viewModel.onYearSelected(2024)

        assertEquals(2024, viewModel.selectedYear.value)
    }

    @Test
    fun `onYearSelected with null clears year filter`() = runTest {
        val viewModel = createViewModel()

        viewModel.onYearSelected(2024)
        viewModel.onYearSelected(null)

        assertEquals(null, viewModel.selectedYear.value)
    }

    @Test
    fun `savedStateHandle restores search query and year`() = runTest {
        val handle = SavedStateHandle(mapOf("search_query" to "batman", "selected_year" to 2023))
        val viewModel = createViewModel(savedStateHandle = handle)

        assertEquals("batman", viewModel.searchQuery.value)
        assertEquals(2023, viewModel.selectedYear.value)
    }

    @Test
    fun `recentSearches emits values from use case`() = runTest {
        val searches = listOf("batman", "avengers", "spider")
        val viewModel = createViewModel(recentSearches = searches)

        viewModel.recentSearches.test {
            // stateIn 초기값 emptyList() 또는 바로 searches
            val item = awaitItem()
            if (item.isEmpty()) {
                assertEquals(searches, awaitItem())
            } else {
                assertEquals(searches, item)
            }
        }
    }
}
