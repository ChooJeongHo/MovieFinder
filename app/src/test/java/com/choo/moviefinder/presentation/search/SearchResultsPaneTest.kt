package com.choo.moviefinder.presentation.search

import androidx.paging.LoadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SearchResultsPaneTest {

    private val idle = LoadState.NotLoading(endOfPaginationReached = false)
    private val ended = LoadState.NotLoading(endOfPaginationReached = true)

    @Test
    fun `refresh loading shows loading even when items exist`() {
        assertEquals(SearchResultsPane.Loading, resolveSearchResultsPane(LoadState.Loading, idle, itemCount = 0))
        assertEquals(SearchResultsPane.Loading, resolveSearchResultsPane(LoadState.Loading, idle, itemCount = 20))
    }

    @Test
    fun `refresh error surfaces the refresh cause`() {
        val cause = IOException("refresh")

        val pane = resolveSearchResultsPane(LoadState.Error(cause), idle, itemCount = 0)

        assertSame(cause, (pane as SearchResultsPane.Error).cause)
    }

    @Test
    fun `items present shows results regardless of append state`() {
        listOf(idle, ended, LoadState.Loading, LoadState.Error(IOException("append"))).forEach { append ->
            assertEquals(SearchResultsPane.Results, resolveSearchResultsPane(idle, append, itemCount = 1))
        }
    }

    // 필터로 첫 페이지가 통째로 걸러진 상태(refresh NotLoading + 0건)에서 다음 페이지를 불러오는 중 — 버그였던 지점
    @Test
    fun `empty list while append is loading is still loading not empty`() {
        assertEquals(SearchResultsPane.Loading, resolveSearchResultsPane(idle, LoadState.Loading, itemCount = 0))
    }

    // 페이지와 페이지 사이에 append가 NotLoading(end=false)로 잠깐 내려오는 구간 (ms 단위)
    @Test
    fun `empty list between chained page loads is loading not empty`() {
        assertEquals(SearchResultsPane.Loading, resolveSearchResultsPane(idle, idle, itemCount = 0))
    }

    @Test
    fun `empty list is no results only after pagination ended`() {
        assertEquals(SearchResultsPane.NoResults, resolveSearchResultsPane(idle, ended, itemCount = 0))
    }

    @Test
    fun `empty list with failed append shows error so the user can retry`() {
        val cause = IOException("append")

        val pane = resolveSearchResultsPane(idle, LoadState.Error(cause), itemCount = 0)

        assertTrue(pane is SearchResultsPane.Error)
        assertSame(cause, (pane as SearchResultsPane.Error).cause)
    }
}
