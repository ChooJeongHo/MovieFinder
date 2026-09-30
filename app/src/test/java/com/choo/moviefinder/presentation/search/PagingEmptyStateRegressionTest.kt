package com.choo.moviefinder.presentation.search

import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.choo.moviefinder.data.paging.BaseMoviePagingSource
import com.choo.moviefinder.data.remote.dto.MovieDto
import com.choo.moviefinder.data.remote.dto.MovieListResponse
import com.choo.moviefinder.domain.model.KoreanRating
import com.choo.moviefinder.domain.model.KoreanRatingGrade
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.usecase.FilterMoviesByKoreanRatingUseCase
import com.choo.moviefinder.domain.usecase.GetKoreanRatingUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

// 실제 앱 파이프라인(등급 변경 시 재검색: Pager → FilterMoviesByKoreanRatingUseCase → cachedIn → collectLatest →
// PagingDataPresenter)을 그대로 돌려, "한 페이지가 통째로 걸러져도 Paging이 다음 페이지를 스스로 이어 로드하고,
// 그동안 UI 판정은 '결과 없음'이 아니라 '찾는 중'이어야 한다"는 계약을 고정한다.
// resolveSearchResultsPane이 이 전제(자동 이어 로드)에 기대므로, 전제가 깨지면 이 테스트가 먼저 실패한다.
// 필터를 cachedIn 뒤(combine)에 두면 2페이지 선로드 케이스에서 이어 로드가 멈춘다 — 그 구조로 되돌리지 말 것.
class PagingEmptyStateRegressionTest {

    // fetchedBeforeFilter: 등급을 고르기 전(필터 없는 화면)에 이미 받은 페이지 수. 등급을 고르면 새 Pager가 1페이지부터
    // 다시 받으므로, 필터 구간의 연쇄 로드는 fetchedPages 전체가 아니라 filterFetches로 비교한다.
    private class Result(
        val fetchedPages: List<Int>,
        val fetchedBeforeFilter: Int,
        val panes: List<SearchResultsPane>,
        val finalItemCount: Int
    ) {
        val filterFetches: List<Int> get() = fetchedPages.drop(fetchedBeforeFilter)
    }

    private class FakeSource(
        private val totalPages: Int,
        private val resultsPerPage: Int,
        private val failOnPage: Int?,
        private val fetched: MutableList<Int>
    ) : BaseMoviePagingSource() {
        override suspend fun fetchPage(page: Int): MovieListResponse {
            fetched += page
            if (page == failOnPage) throw IOException("page $page")
            return MovieListResponse(
                page = page,
                results = (1..resultsPerPage).map { dto((page - 1) * PAGE + it) },
                totalPages = totalPages,
                totalResults = totalPages * resultsPerPage
            )
        }
    }

    private class NoopUpdate : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) = Unit
        override fun onRemoved(position: Int, count: Int) = Unit
        override fun onMoved(fromPosition: Int, toPosition: Int) = Unit
        override fun onChanged(position: Int, count: Int, payload: Any?) = Unit
    }

    private val diffCallback = object : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie) = oldItem == newItem
    }

    // startWithFilter=false: 필터 없이 먼저 결과를 받은 뒤 등급 칩을 고르는 실제 사용자 흐름
    // stopWhen: 필터 적용 후 관찰을 끝낼 조건 (판정, 지금까지 가져온 페이지)
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observe(
        totalPages: Int,
        passPages: Set<Int>,
        startWithFilter: Boolean,
        resultsPerPage: Int = PAGE,
        failOnPage: Int? = null,
        preloadSecondPage: Boolean = false,
        stopWhen: (SearchResultsPane, List<Int>) -> Boolean
    ): Result = runBlocking {
        val fetched: MutableList<Int> = Collections.synchronizedList(mutableListOf())
        val rating = mockk<GetKoreanRatingUseCase>()
        coEvery { rating(any()) } coAnswers {
            delay(ITEM_DELAY_MS)
            val id = firstArg<String>().removePrefix("m").toInt()
            if ((id - 1) / PAGE + 1 in passPages) KoreanRating(GRADE.apiGradeName, "t", "p", "d", "2024", "r") else null
        }
        val scope = CoroutineScope(Dispatchers.Default + Job())
        val newPager = {
            Pager(
                config = PagingConfig(
                    pageSize = PAGE,
                    prefetchDistance = 5,
                    initialLoadSize = 60,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = { FakeSource(totalPages, resultsPerPage, failOnPage, fetched) }
            ).flow
        }
        val grade = MutableStateFlow<KoreanRatingGrade?>(if (startWithFilter) GRADE else null)
        val filterUseCase = FilterMoviesByKoreanRatingUseCase(rating)
        // 앱 구조(SearchViewModel.searchResults): 등급이 바뀌면 재검색(새 Pager)하고 필터는 cachedIn 앞에 둔다
        val results = grade.flatMapLatest { g -> newPager().map { filterUseCase(it, g) } }.cachedIn(scope)

        val differ = AsyncPagingDataDiffer(diffCallback, NoopUpdate(), Dispatchers.Default, Dispatchers.Default)
        val panes: MutableList<SearchResultsPane> = Collections.synchronizedList(mutableListOf())
        val filtering = AtomicBoolean(startWithFilter)
        val unfilteredShown = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        var fetchedBeforeFilter = 0
        differ.addLoadStateListener { states ->
            val pane = resolveSearchResultsPane(states.refresh, states.append, differ.itemCount)
            if (!filtering.get()) {
                if (pane == SearchResultsPane.Results) unfilteredShown.complete(Unit)
            } else if (panes.lastOrNull() != pane) {
                panes += pane
                if (stopWhen(pane, fetched.toList())) finished.complete(Unit)
            }
        }
        // LazyPagingItems와 동일하게 collectLatest로 소비한다.
        val collectJob = scope.launch { results.collectLatest { differ.submitData(it) } }
        withTimeout(TIMEOUT_MS) {
            if (!startWithFilter) {
                unfilteredShown.await()
                if (preloadSecondPage) {
                    // 사용자가 스크롤해 2페이지가 이미 로드된 상태에서 등급 칩을 고르는 흐름
                    differ.getItem(differ.itemCount - 1)
                    while (differ.itemCount < 2 * PAGE) delay(POLL_MS)
                    // 등급 선택으로 목록이 갈아끼워지면 뷰포트는 맨 위로 돌아간다고 본다. 이걸 빼면 위에서 접근한
                    // '끝' 인덱스가 새 세대에 남아 4페이지 도착 직후 5페이지를 한 번 더 프리페치한다(확인됨: 정확히
                    // [1,2,3,4,5]) — 빈 페이지 연쇄와 무관한 하네스 부산물이라 연쇄 계약(1~4 순차)만 검증한다.
                    differ.getItem(0)
                }
                fetchedBeforeFilter = fetched.size
                filtering.set(true)
                grade.value = GRADE
            }
            finished.await()
        }
        val outcome = Result(fetched.toList(), fetchedBeforeFilter, panes.toList(), differ.itemCount)
        collectJob.cancel()
        scope.cancel()
        outcome
    }

    @Test
    fun `filter from start - first pages filtered out keeps loading until a matching page arrives`() {
        val result = observe(totalPages = 5, passPages = setOf(3), startWithFilter = true) { pane, _ ->
            pane == SearchResultsPane.Results
        }

        assertEquals(listOf(1, 2, 3), result.filterFetches)
        assertFalse("빈 페이지 연쇄 로드 중에 결과 없음이 노출됨", result.panes.contains(SearchResultsPane.NoResults))
        assertEquals(SearchResultsPane.Results, result.panes.last())
        assertTrue(result.finalItemCount > 0)
    }

    @Test
    fun `chip selected after results loaded - filtered-out pages keep loading until a matching page arrives`() {
        val result = observe(totalPages = 5, passPages = setOf(3), startWithFilter = false) { pane, fetched ->
            pane == SearchResultsPane.Results && 3 in fetched
        }

        assertEquals(listOf(1, 2, 3), result.filterFetches)
        assertFalse("빈 페이지 연쇄 로드 중에 결과 없음이 노출됨", result.panes.contains(SearchResultsPane.NoResults))
        assertEquals(SearchResultsPane.Results, result.panes.last())
    }

    @Test
    fun `two pages already loaded then both filtered out - keeps loading until a matching page arrives`() {
        val result = observe(
            totalPages = 5,
            passPages = setOf(4),
            startWithFilter = false,
            preloadSecondPage = true
        ) { pane, fetched -> pane == SearchResultsPane.Results && 4 in fetched }

        assertEquals(listOf(1, 2, 3, 4), result.filterFetches)
        assertFalse("빈 페이지 연쇄 로드 중에 결과 없음이 노출됨", result.panes.contains(SearchResultsPane.NoResults))
        assertEquals(SearchResultsPane.Results, result.panes.last())
    }

    @Test
    fun `no page matches - no results appears only after the last page`() {
        val result = observe(totalPages = 3, passPages = emptySet(), startWithFilter = false) { pane, _ ->
            pane == SearchResultsPane.NoResults
        }

        assertEquals(listOf(1, 2, 3), result.filterFetches)
        assertEquals(SearchResultsPane.NoResults, result.panes.last())
        assertFalse(
            "마지막 페이지 전에 결과 없음이 노출됨",
            result.panes.dropLast(1).contains(SearchResultsPane.NoResults)
        )
        assertEquals(0, result.finalItemCount)
    }

    @Test
    fun `search with zero results reports no results right away without chaining`() {
        val result = observe(
            totalPages = 0,
            passPages = emptySet(),
            startWithFilter = true,
            resultsPerPage = 0
        ) { pane, _ -> pane == SearchResultsPane.NoResults }

        assertEquals(listOf(1), result.filterFetches)
        assertEquals(SearchResultsPane.NoResults, result.panes.last())
    }

    @Test
    fun `append failure while empty stops the chain and surfaces an error instead of no results`() {
        val result = observe(
            totalPages = 5,
            passPages = setOf(4),
            startWithFilter = true,
            failOnPage = 2
        ) { pane, _ -> pane is SearchResultsPane.Error }

        assertEquals(listOf(1, 2), result.filterFetches)
        assertFalse(result.panes.contains(SearchResultsPane.NoResults))
        assertTrue(result.panes.last() is SearchResultsPane.Error)
    }

    private companion object {
        val GRADE = KoreanRatingGrade.TWELVE_AND_UP
        const val PAGE = 20
        const val ITEM_DELAY_MS = 2L
        const val TIMEOUT_MS = 30_000L
        const val POLL_MS = 5L

        fun dto(id: Int) = MovieDto(
            id = id, title = "m$id", posterPath = "/p.jpg", backdropPath = "/b.jpg",
            overview = "o", releaseDate = "2024-01-01", voteAverage = 7.0, voteCount = 1
        )
    }
}
