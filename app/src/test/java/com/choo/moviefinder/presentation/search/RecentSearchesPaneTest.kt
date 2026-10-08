package com.choo.moviefinder.presentation.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RecentSearchesPaneTest {

    private val all = listOf("batman", "superman", "spiderman")

    // 검색어 제출 후 X로 지우기: 필터된 빈 목록이 남던 지점. 이 함수는 상태가 없어서 "어댑터에 다시 넣는" 동작 자체는
    // Fragment 연결부(updateVisibility의 apply 호출) 몫이고, 여기서는 blank 입력이 전체 목록으로 판정되는 것만 고정한다.
    @Test
    fun `an empty filtered result hides the pane and a blank query resolves to the full list`() {
        assertEquals(RecentSearchesPane.Hidden, resolveRecentSearchesPane(all, "zzz", hasGenreFilter = false))
        assertEquals(RecentSearchesPane.Recent(all), resolveRecentSearchesPane(all, "", hasGenreFilter = false))
    }

    // 부분 목록이 남아 있다가 입력을 지우면 전체 목록 — 위와 같은 뿌리의 버그(판정 결과만 고정, 연결부는 위 주석 참고)
    @Test
    fun `a partial filtered result is followed by the full list once the query is blank`() {
        val searches = listOf("bat", "batman", "superman")

        assertEquals(
            RecentSearchesPane.Filtered(listOf("batman")),
            resolveRecentSearchesPane(searches, "bat", hasGenreFilter = false)
        )
        assertEquals(
            RecentSearchesPane.Recent(searches),
            resolveRecentSearchesPane(searches, "", hasGenreFilter = false)
        )
    }

    // 필터 결과가 비었는데 영역이 남아 "최근 검색어" 제목만 덩그러니 보이던 지점
    @Test
    fun `empty filter result hides the pane`() {
        // (a) 일치하는 항목이 없다
        assertEquals(RecentSearchesPane.Hidden, resolveRecentSearchesPane(all, "zzz", hasGenreFilter = false))
        // (b) 유일한 일치 항목이 입력과 똑같아서 빠진다
        assertEquals(
            RecentSearchesPane.Hidden,
            resolveRecentSearchesPane(listOf("batman"), "batman", hasGenreFilter = false)
        )
    }

    @Test
    fun `non-blank query with no recent searches hides the pane`() {
        assertEquals(RecentSearchesPane.Hidden, resolveRecentSearchesPane(emptyList(), "bat", hasGenreFilter = false))
    }

    @Test
    fun `blank query with no recent searches and no genre shows the initial state`() {
        assertEquals(RecentSearchesPane.Initial, resolveRecentSearchesPane(emptyList(), "", hasGenreFilter = false))
    }

    @Test
    fun `blank query with a genre selected keeps visibility and swaps the list only`() {
        assertEquals(
            RecentSearchesPane.KeepVisibility(all),
            resolveRecentSearchesPane(all, "", hasGenreFilter = true)
        )
        // 목록이 비어 있어도 장르 탐색 화면을 초기 안내로 덮어쓰지 않는다
        val empty = resolveRecentSearchesPane(emptyList(), "", hasGenreFilter = true)
        assertEquals(RecentSearchesPane.KeepVisibility(emptyList()), empty)
        assertNotEquals(RecentSearchesPane.Initial, empty)
    }

    @Test
    fun `genre selection does not change how a non-blank query is resolved`() {
        assertEquals(
            resolveRecentSearchesPane(listOf("batman", "superman"), "bat", hasGenreFilter = false),
            resolveRecentSearchesPane(listOf("batman", "superman"), "bat", hasGenreFilter = true)
        )
    }

    @Test
    fun `matching ignores case`() {
        assertEquals(
            RecentSearchesPane.Filtered(listOf("Batman")),
            resolveRecentSearchesPane(listOf("Batman"), "bat", hasGenreFilter = false)
        )
    }

    @Test
    fun `an entry identical to the query is dropped`() {
        assertEquals(
            RecentSearchesPane.Filtered(listOf("batman begins")),
            resolveRecentSearchesPane(listOf("batman", "batman begins"), "batman", hasGenreFilter = false)
        )
    }

    // 동일 항목 제외는 대소문자를 구분한다(!=) — 현재 시맨틱을 고정
    @Test
    fun `an entry that differs from the query only by case is kept`() {
        assertEquals(
            RecentSearchesPane.Filtered(listOf("Batman")),
            resolveRecentSearchesPane(listOf("Batman"), "batman", hasGenreFilter = false)
        )
    }

    @Test
    fun `query is trimmed and whitespace only counts as blank`() {
        assertEquals(
            RecentSearchesPane.Filtered(listOf("batman begins")),
            resolveRecentSearchesPane(listOf("batman", "batman begins"), "  batman ", hasGenreFilter = false)
        )
        assertEquals(RecentSearchesPane.Recent(all), resolveRecentSearchesPane(all, "   ", hasGenreFilter = false))
    }

    @Test
    fun `a substring in the middle of an entry matches`() {
        assertEquals(
            RecentSearchesPane.Filtered(listOf("the batman")),
            resolveRecentSearchesPane(listOf("the batman"), "bat", hasGenreFilter = false)
        )
    }

    @Test
    fun `filtered entries keep their original order`() {
        assertEquals(
            RecentSearchesPane.Filtered(listOf("batman begins", "the batman", "batmobile")),
            resolveRecentSearchesPane(
                listOf("batman begins", "superman", "the batman", "batmobile"),
                "bat",
                hasGenreFilter = false
            )
        )
    }
}
