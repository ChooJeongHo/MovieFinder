package com.choo.moviefinder.presentation.search

import androidx.paging.PagingData
import app.cash.turbine.test
import com.choo.moviefinder.domain.model.Genre
import com.choo.moviefinder.domain.model.KoreanRatingGrade
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.usecase.ClearSearchHistoryUseCase
import com.choo.moviefinder.domain.usecase.DeleteSearchQueryUseCase
import com.choo.moviefinder.domain.usecase.DiscoverMoviesUseCase
import com.choo.moviefinder.domain.usecase.FilterMoviesByKoreanRatingUseCase
import com.choo.moviefinder.domain.usecase.GetGenreListUseCase
import com.choo.moviefinder.domain.usecase.GetRecentSearchesUseCase
import com.choo.moviefinder.domain.usecase.GetWatchHistoryUseCase
import com.choo.moviefinder.domain.usecase.SaveSearchQueryUseCase
import com.choo.moviefinder.domain.usecase.SearchLocalMoviesUseCase
import com.choo.moviefinder.domain.usecase.SearchMoviesUseCase
import com.choo.moviefinder.domain.usecase.SearchPersonUseCase
import androidx.lifecycle.SavedStateHandle
import com.choo.moviefinder.util.CoroutineTestBase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import com.choo.moviefinder.presentation.adapter.MoviePagingAdapter.ViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest : CoroutineTestBase() {

    private lateinit var searchMoviesUseCase: SearchMoviesUseCase
    private lateinit var discoverMoviesUseCase: DiscoverMoviesUseCase
    private lateinit var getGenreListUseCase: GetGenreListUseCase
    private lateinit var getRecentSearchesUseCase: GetRecentSearchesUseCase
    private lateinit var saveSearchQueryUseCase: SaveSearchQueryUseCase
    private lateinit var deleteSearchQueryUseCase: DeleteSearchQueryUseCase
    private lateinit var clearSearchHistoryUseCase: ClearSearchHistoryUseCase
    private lateinit var searchPersonUseCase: SearchPersonUseCase
    private lateinit var searchLocalMoviesUseCase: SearchLocalMoviesUseCase
    private lateinit var filterMoviesByKoreanRatingUseCase: FilterMoviesByKoreanRatingUseCase
    private lateinit var getWatchHistoryUseCase: GetWatchHistoryUseCase

    @Before
    fun setup() {
        searchMoviesUseCase = mockk()
        discoverMoviesUseCase = mockk()
        getGenreListUseCase = mockk()
        getRecentSearchesUseCase = mockk()
        saveSearchQueryUseCase = mockk()
        deleteSearchQueryUseCase = mockk()
        clearSearchHistoryUseCase = mockk()
        searchPersonUseCase = mockk()
        searchLocalMoviesUseCase = mockk()
        filterMoviesByKoreanRatingUseCase = mockk()
        getWatchHistoryUseCase = mockk()

        coEvery { getGenreListUseCase() } returns emptyList()
        coEvery { searchLocalMoviesUseCase(any()) } returns emptyList()
        coEvery { filterMoviesByKoreanRatingUseCase(any(), any()) } answers { firstArg() }
        every { getWatchHistoryUseCase() } returns flowOf(emptyList())
    }

    private fun createViewModel(
        recentSearches: List<String> = emptyList(),
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ): SearchViewModel {
        every { getRecentSearchesUseCase() } returns flowOf(recentSearches)
        return SearchViewModel(
            savedStateHandle = savedStateHandle,
            searchMoviesUseCase = searchMoviesUseCase,
            discoverMoviesUseCase = discoverMoviesUseCase,
            getGenreListUseCase = getGenreListUseCase,
            getRecentSearchesUseCase = getRecentSearchesUseCase,
            saveSearchQueryUseCase = saveSearchQueryUseCase,
            deleteSearchQueryUseCase = deleteSearchQueryUseCase,
            clearSearchHistoryUseCase = clearSearchHistoryUseCase,
            searchPersonUseCase = searchPersonUseCase,
            searchLocalMoviesUseCase = searchLocalMoviesUseCase,
            filterMoviesByKoreanRatingUseCase = filterMoviesByKoreanRatingUseCase,
            getWatchHistoryUseCase = getWatchHistoryUseCase
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

    // --- Genre/Sort filters ---

    @Test
    fun `onGenresSelected updates selectedGenres`() = runTest {
        val viewModel = createViewModel()

        viewModel.onGenresSelected(setOf(28, 35))

        assertEquals(setOf(28, 35), viewModel.selectedGenres.value)
    }

    @Test
    fun `onGenresSelected with empty set clears genres`() = runTest {
        val viewModel = createViewModel()

        viewModel.onGenresSelected(setOf(28))
        viewModel.onGenresSelected(emptySet())

        assertTrue(viewModel.selectedGenres.value.isEmpty())
    }

    @Test
    fun `onSortSelected updates sortBy`() = runTest {
        val viewModel = createViewModel()

        viewModel.onSortSelected(SortOption.VOTE_AVERAGE_DESC)

        assertEquals(SortOption.VOTE_AVERAGE_DESC, viewModel.sortBy.value)
    }

    @Test
    fun `genres emits loaded genres from use case`() = runTest {
        val genreList = listOf(Genre(28, "Action"), Genre(35, "Comedy"))
        coEvery { getGenreListUseCase() } returns genreList

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.genres.test {
            assertEquals(genreList, awaitItem())
        }
    }

    @Test
    fun `SortOption apiValue maps correctly`() {
        assertEquals("popularity.desc", SortOption.POPULARITY_DESC.apiValue)
        assertEquals("vote_average.desc", SortOption.VOTE_AVERAGE_DESC.apiValue)
        assertEquals("release_date.desc", SortOption.RELEASE_DATE_DESC.apiValue)
        assertEquals("revenue.desc", SortOption.REVENUE_DESC.apiValue)
    }

    @Test
    fun `initial sortBy is POPULARITY_DESC`() = runTest {
        val viewModel = createViewModel()

        assertEquals(SortOption.POPULARITY_DESC, viewModel.sortBy.value)
    }

    @Test
    fun `savedStateHandle restores genres and sort`() = runTest {
        val handle = SavedStateHandle(
            mapOf(
                "search_query" to "test",
                "selected_genres" to intArrayOf(28, 35),
                "selected_sort" to "VOTE_AVERAGE_DESC"
            )
        )
        val viewModel = createViewModel(savedStateHandle = handle)

        assertEquals(setOf(28, 35), viewModel.selectedGenres.value)
        assertEquals(SortOption.VOTE_AVERAGE_DESC, viewModel.sortBy.value)
    }

    @Test
    fun `onGenresSelected saves to savedStateHandle`() = runTest {
        val handle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle = handle)

        viewModel.onGenresSelected(setOf(28, 12))

        val saved = handle.get<IntArray>("selected_genres")
        assertTrue(saved != null && saved.toSet() == setOf(28, 12))
    }

    @Test
    fun `onSortSelected saves to savedStateHandle`() = runTest {
        val handle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle = handle)

        viewModel.onSortSelected(SortOption.RELEASE_DATE_DESC)

        assertEquals("RELEASE_DATE_DESC", handle.get<String>("selected_sort"))
    }

    @Test
    fun `onSearch trims whitespace`() = runTest {
        coEvery { saveSearchQueryUseCase(any()) } returns Unit
        val viewModel = createViewModel()

        viewModel.onSearch("  avengers  ")
        advanceUntilIdle()

        coVerify { saveSearchQueryUseCase("avengers") }
    }

    // --- View Mode Toggle ---

    @Test
    fun `initial viewMode is GRID`() = runTest {
        val viewModel = createViewModel()

        assertEquals(ViewMode.GRID, viewModel.viewMode.value)
    }

    @Test
    fun `toggleViewMode switches GRID to LIST`() = runTest {
        val viewModel = createViewModel()

        viewModel.toggleViewMode()

        assertEquals(ViewMode.LIST, viewModel.viewMode.value)
    }

    @Test
    fun `toggleViewMode switches LIST back to GRID`() = runTest {
        val viewModel = createViewModel()

        viewModel.toggleViewMode()
        viewModel.toggleViewMode()

        assertEquals(ViewMode.GRID, viewModel.viewMode.value)
    }

    @Test
    fun `toggleViewMode saves to savedStateHandle`() = runTest {
        val handle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle = handle)

        viewModel.toggleViewMode()

        assertEquals("LIST", handle.get<String>("view_mode"))
    }

    // --- Discover / Genre Retry ---

    @Test
    fun `onDiscoverWithFilters does nothing when no genres selected`() = runTest {
        val viewModel = createViewModel()

        viewModel.onDiscoverWithFilters()
        advanceUntilIdle()

        // genres가 비어있으면 즉시 return → discoverMoviesUseCase 호출 안됨
        coVerify(exactly = 0) { discoverMoviesUseCase(any(), any(), any()) }
    }

    @Test
    fun `retryLoadGenres retries after failure`() = runTest {
        val genreList = listOf(com.choo.moviefinder.domain.model.Genre(28, "Action"))
        coEvery { getGenreListUseCase() } throws RuntimeException("fail") andThen genreList

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.genres.value.isEmpty())

        viewModel.retryLoadGenres()
        advanceUntilIdle()

        assertEquals(genreList, viewModel.genres.value)
    }

    @Test
    fun `retryLoadGenres does nothing when genres loaded successfully`() = runTest {
        val genreList = listOf(com.choo.moviefinder.domain.model.Genre(28, "Action"))
        coEvery { getGenreListUseCase() } returns genreList

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.retryLoadGenres()
        advanceUntilIdle()

        // init에서 1번만 호출, retryLoadGenres는 genreLoadFailed=false이므로 재호출 안됨
        coVerify(exactly = 1) { getGenreListUseCase() }
    }

    // --- Rating Grade Filter ---

    @Test
    fun `initial selectedRatingGrade is null`() = runTest {
        val viewModel = createViewModel()

        assertEquals(null, viewModel.selectedRatingGrade.value)
    }

    @Test
    fun `onRatingGradeSelected updates selectedRatingGrade`() = runTest {
        val viewModel = createViewModel()

        viewModel.onRatingGradeSelected(KoreanRatingGrade.FIFTEEN_AND_UP)

        assertEquals(KoreanRatingGrade.FIFTEEN_AND_UP, viewModel.selectedRatingGrade.value)
    }

    @Test
    fun `onRatingGradeSelected saves to savedStateHandle`() = runTest {
        val handle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle = handle)

        viewModel.onRatingGradeSelected(KoreanRatingGrade.FIFTEEN_AND_UP)

        assertEquals("FIFTEEN_AND_UP", handle.get<String>("selected_rating_grade"))
    }

    @Test
    fun `onRatingGradeSelected with null clears savedStateHandle value`() = runTest {
        val handle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle = handle)

        viewModel.onRatingGradeSelected(KoreanRatingGrade.FIFTEEN_AND_UP)
        viewModel.onRatingGradeSelected(null)

        assertEquals(null, viewModel.selectedRatingGrade.value)
        assertEquals(null, handle.get<String>("selected_rating_grade"))
    }

    @Test
    fun `savedStateHandle restores selectedRatingGrade`() = runTest {
        val handle = SavedStateHandle(mapOf("selected_rating_grade" to "FIFTEEN_AND_UP"))
        val viewModel = createViewModel(savedStateHandle = handle)

        assertEquals(KoreanRatingGrade.FIFTEEN_AND_UP, viewModel.selectedRatingGrade.value)
    }

    // 회귀 방지 핵심: 등급 필터만 변경해도 TMDB 재검색이 발생하면 안 된다 (cachedIn 이후 결합 검증)
    @Test
    fun `changing rating grade after search does not trigger a new TMDB search`() = runTest {
        val movies = listOf(Movie(1, "테스트 영화", null, null, "overview", "2024-01-01", 7.5, 100))
        coEvery { saveSearchQueryUseCase(any()) } returns Unit
        coEvery { searchMoviesUseCase("avengers", null) } returns flowOf(PagingData.from(movies))

        val viewModel = createViewModel()

        viewModel.searchResults.test {
            // 컬렉터가 실제로 구독을 마칠 때까지 스케줄러를 먼저 비운다 — 그렇지 않으면
            // onSearch()의 _immediateSearch.tryEmit()이 구독 이전에 발생해 유실된다
            // (replay=0 SharedFlow).
            runCurrent()
            viewModel.onSearch("avengers")
            advanceUntilIdle()
            awaitItem()

            viewModel.onRatingGradeSelected(KoreanRatingGrade.FIFTEEN_AND_UP)
            advanceUntilIdle()
            awaitItem()

            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { searchMoviesUseCase(any(), any()) }
    }
}
