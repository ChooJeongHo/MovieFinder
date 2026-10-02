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
import com.choo.moviefinder.domain.usecase.GetLocalizedTitleUseCase
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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
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
    private lateinit var getLocalizedTitleUseCase: GetLocalizedTitleUseCase
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
        getLocalizedTitleUseCase = mockk()
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
            getLocalizedTitleUseCase = getLocalizedTitleUseCase,
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

    // 회귀 방지 핵심: 필터는 cachedIn 앞에 있어야 하므로 등급을 바꾸면 재검색(새 Pager)이 일어나고, 새 등급이 필터에 전달된다.
    // cachedIn 뒤에 결합하면 2페이지 선로드 후 전부 걸러질 때 이어 로드가 멈춘다(PagingEmptyStateRegressionTest 참고).
    @Test
    fun `changing rating grade after search re-searches once and filters with the new grade`() = runTest {
        val movies = listOf(Movie(1, "테스트 영화", null, null, "overview", "2024-01-01", 7.5, 100))
        coEvery { searchMoviesUseCase("avengers", null) } returns flowOf(PagingData.from(movies))

        val viewModel = createViewModel()

        viewModel.searchResults.test {
            // 실제 사용 흐름대로 타이핑(debounce)으로 검색한다. onSearch()만 단독 호출하면 _searchQuery가 비어 있어
            // 뒤늦게 도착하는 debounce의 빈 초기 파라미터가 '최신 파라미터'를 덮어쓴다(앱은 항상 쿼리를 먼저 갱신함).
            viewModel.onSearchQueryChange("avengers")
            advanceUntilIdle()
            awaitItem()
            coVerify(exactly = 1) { searchMoviesUseCase(any(), any()) }
            coVerify(exactly = 1) { filterMoviesByKoreanRatingUseCase(any(), null) }

            viewModel.onRatingGradeSelected(KoreanRatingGrade.FIFTEEN_AND_UP)
            advanceUntilIdle()
            awaitItem()

            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 2) { searchMoviesUseCase("avengers", null) }
        coVerify(exactly = 1) { filterMoviesByKoreanRatingUseCase(any(), KoreanRatingGrade.FIFTEEN_AND_UP) }
    }

    // 검색어/장르가 없는 상태의 등급 변경은 검색할 대상이 없으므로 재검색(=KMRB/TMDB 호출)을 일으키면 안 된다
    @Test
    fun `changing rating grade without a query or genres does not trigger a search`() = runTest {
        val viewModel = createViewModel()

        viewModel.searchResults.test {
            runCurrent()
            advanceUntilIdle() // debounce된 초기 빈 파라미터가 filter에서 걸러질 시간을 준다

            viewModel.onRatingGradeSelected(KoreanRatingGrade.FIFTEEN_AND_UP)
            advanceUntilIdle()

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { searchMoviesUseCase(any(), any()) }
        coVerify(exactly = 0) { discoverMoviesUseCase(any(), any(), any()) }
    }

    // ── 즉시 검색과 debounce 검색의 중복 방지 ──────────────────────────────────────────────────────────
    // 두 경로(타이핑 debounce / 엔터·칩 클릭 즉시)가 같은 조건을 연달아 내보내면 flatMapLatest가 새 Pager를 두 번
    // 만들어 목록 새로고침과 TMDB/KMRB 호출이 중복된다. dedupe는 merge 뒤(합류 지점)에 있어야 가지 사이의 중복까지
    // 막히고, 검색어는 두 경로 모두 trim해서 비교해야 "batman "과 "batman"이 같은 검색으로 취급된다.
    // 아래 호출 순서는 SearchFragment의 실제 핸들러(칩 클릭 / IME 검색 / 장르 다이얼로그 확인)를 그대로 따른다.

    private val dedupeFixtureMovies = listOf(Movie(1, "테스트 영화", null, null, "overview", "2024-01-01", 7.5, 100))

    private fun stubSearchAndDiscover() {
        coEvery { searchMoviesUseCase(any(), any()) } returns flowOf(PagingData.from(dedupeFixtureMovies))
        coEvery { discoverMoviesUseCase(any(), any(), any()) } returns flowOf(PagingData.from(dedupeFixtureMovies))
        coEvery { saveSearchQueryUseCase(any()) } returns Unit
    }

    // searchResults를 구독해야 검색 파이프라인이 돈다. 구독 직후(초기 debounce 이전)에서 block을 실행한다.
    private suspend fun TestScope.collectingSearchResults(
        viewModel: SearchViewModel,
        block: suspend TestScope.() -> Unit
    ) {
        val scope = this
        viewModel.searchResults.test {
            scope.runCurrent()
            scope.block()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `chip click searches once even though query change and immediate search both fire`() = runTest {
        stubSearchAndDiscover()
        val viewModel = createViewModel()

        collectingSearchResults(viewModel) {
            viewModel.onSearchQueryChange("batman")
            viewModel.onSearch("batman")
            advanceUntilIdle()
        }

        coVerify(exactly = 1) { searchMoviesUseCase(any(), any()) }
        coVerify(exactly = 1) { searchMoviesUseCase("batman", null) }
    }

    @Test
    fun `enter within the debounce window searches once`() = runTest {
        stubSearchAndDiscover()
        val viewModel = createViewModel()

        collectingSearchResults(viewModel) {
            viewModel.onSearchQueryChange("batman")
            advanceTimeBy(100) // debounce(300ms) 만료 전
            viewModel.onSearch("batman")
            advanceUntilIdle() // 뒤늦게 만료되는 debounce가 같은 조건을 또 내보내는 구간
        }

        coVerify(exactly = 1) { searchMoviesUseCase(any(), any()) }
    }

    @Test
    fun `enter after the debounce search already fired searches once`() = runTest {
        stubSearchAndDiscover()
        val viewModel = createViewModel()

        collectingSearchResults(viewModel) {
            viewModel.onSearchQueryChange("batman")
            advanceTimeBy(1_000) // 자동 검색이 이미 발화
            viewModel.onSearch("batman")
            advanceUntilIdle()
        }

        coVerify(exactly = 1) { searchMoviesUseCase(any(), any()) }
    }

    @Test
    fun `query with trailing space is searched once and always trimmed`() = runTest {
        stubSearchAndDiscover()
        val viewModel = createViewModel()

        collectingSearchResults(viewModel) {
            viewModel.onSearchQueryChange("batman ")
            advanceTimeBy(1_000)
            viewModel.onSearch("batman ")
            advanceUntilIdle()
        }

        coVerify(exactly = 1) { searchMoviesUseCase(any(), any()) }
        coVerify(exactly = 1) { searchMoviesUseCase("batman", null) }
    }

    @Test
    fun `genre dialog confirm with blank query discovers once`() = runTest {
        stubSearchAndDiscover()
        val viewModel = createViewModel()

        collectingSearchResults(viewModel) {
            viewModel.onGenresSelected(setOf(28))
            viewModel.onDiscoverWithFilters()
            advanceUntilIdle()
        }

        coVerify(exactly = 1) { discoverMoviesUseCase(any(), any(), any()) }
        coVerify(exactly = 1) { discoverMoviesUseCase(setOf(28), any(), any()) }
    }

    // 아래 대조군: 중복 제거가 과해서 정당한 재검색까지 삼키면 안 된다

    @Test
    fun `different queries in a row still search each time`() = runTest {
        stubSearchAndDiscover()
        val viewModel = createViewModel()

        collectingSearchResults(viewModel) {
            viewModel.onSearchQueryChange("batman")
            advanceUntilIdle()
            viewModel.onSearchQueryChange("superman")
            advanceUntilIdle()
        }

        coVerify(exactly = 1) { searchMoviesUseCase("batman", null) }
        coVerify(exactly = 1) { searchMoviesUseCase("superman", null) }
    }

    @Test
    fun `same query typed again after being cleared searches again`() = runTest {
        stubSearchAndDiscover()
        val viewModel = createViewModel()

        collectingSearchResults(viewModel) {
            viewModel.onSearchQueryChange("batman")
            advanceUntilIdle()
            viewModel.onSearchQueryChange("")
            advanceUntilIdle()
            viewModel.onSearchQueryChange("batman")
            advanceUntilIdle()
        }

        coVerify(exactly = 2) { searchMoviesUseCase("batman", null) }
    }

    @Test
    fun `changing the year after a search re-searches with the new year`() = runTest {
        stubSearchAndDiscover()
        val viewModel = createViewModel()

        collectingSearchResults(viewModel) {
            viewModel.onSearchQueryChange("batman")
            advanceUntilIdle()
            viewModel.onYearSelected(2022)
            advanceUntilIdle()
        }

        coVerify(exactly = 1) { searchMoviesUseCase("batman", null) }
        coVerify(exactly = 1) { searchMoviesUseCase("batman", 2022) }
    }

    @Test
    fun `enter with a different query than the auto search runs both searches`() = runTest {
        stubSearchAndDiscover()
        val viewModel = createViewModel()

        collectingSearchResults(viewModel) {
            viewModel.onSearchQueryChange("bat")
            advanceUntilIdle()
            viewModel.onSearchQueryChange("batman")
            viewModel.onSearch("batman")
            advanceUntilIdle()
        }

        coVerify(exactly = 1) { searchMoviesUseCase("bat", null) }
        coVerify(exactly = 1) { searchMoviesUseCase("batman", null) }
    }

    // 엔터가 재검색을 일으키지 않더라도 최근 검색어 저장은 검색 emit과 분리돼 있어 그대로 동작해야 한다
    @Test
    fun `enter on an already searched query still saves it to recent searches`() = runTest {
        stubSearchAndDiscover()
        val viewModel = createViewModel()

        collectingSearchResults(viewModel) {
            viewModel.onSearchQueryChange("batman")
            advanceUntilIdle()
            viewModel.onSearch("batman")
            advanceUntilIdle()
        }

        coVerify(exactly = 1) { searchMoviesUseCase(any(), any()) }
        coVerify(exactly = 1) { saveSearchQueryUseCase("batman") }
    }

    // 표시 제목은 목록(ko-KR title)이 아닌 UseCase 결과를 그대로 노출해야 한다 — 필터는 ko-KR title에 의존하므로
    // ViewModel이 목록의 title 자체를 바꾸면 안 되고, 이 두 함수만 화면 표시용 문자열을 제공한다
    @Test
    fun `peekDisplayTitle delegates to use case with movie id and korean title`() = runTest {
        val movie = Movie(1, "배트맨", null, null, "overview", "2024-01-01", 7.5, 100)
        every { getLocalizedTitleUseCase.peek(1, "배트맨") } returns "The Batman"
        val viewModel = createViewModel()

        assertEquals("The Batman", viewModel.peekDisplayTitle(movie))
    }

    @Test
    fun `resolveDisplayTitle delegates to use case with movie id and korean title`() = runTest {
        val movie = Movie(1, "배트맨", null, null, "overview", "2024-01-01", 7.5, 100)
        coEvery { getLocalizedTitleUseCase(1, "배트맨") } returns "The Batman"
        val viewModel = createViewModel()

        assertEquals("The Batman", viewModel.resolveDisplayTitle(movie))
        coVerify(exactly = 1) { getLocalizedTitleUseCase(1, "배트맨") }
    }
}
