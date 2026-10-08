package com.choo.moviefinder.presentation.search

// 최근 검색어 영역에 넣을 목록과 영역 처리를 (최근 검색어, 입력, 장르 선택 여부)로 정한다.
// SearchFragment의 collector(Flow emit 때)와 updateVisibility(입력이 blank가 될 때)가 같은 판정을 공유해야 한다:
// 검색 제출 후 X로 지우면 Flow가 다시 emit되지 않아, 판정이 둘로 갈리면 필터된 목록이 어댑터에 남는다.
// Hidden은 필터 결과가 비었을 때 "최근 검색어" 제목만 덩그러니 남지 않도록 영역 자체를 숨기는 칸이다.
internal sealed interface RecentSearchesPane {
    val items: List<String>

    // 영역 표시(showRecentSearches)
    data class Recent(override val items: List<String>) : RecentSearchesPane

    // 초기 안내(showInitialState)
    data object Initial : RecentSearchesPane {
        override val items: List<String> get() = emptyList()
    }

    // 장르 탐색 중: 목록만 교체하고 가시성은 건드리지 않는다
    data class KeepVisibility(override val items: List<String>) : RecentSearchesPane

    // 입력 중 필터 목록: 영역만 visible
    data class Filtered(override val items: List<String>) : RecentSearchesPane

    // 입력 중인데 필터 결과가 비었을 때: 영역만 숨김
    data object Hidden : RecentSearchesPane {
        override val items: List<String> get() = emptyList()
    }
}

// 입력은 trim한 값으로 한 번만 판정한다. 장르 선택 여부는 입력이 blank일 때의 분기에만 영향을 준다.
// 필터는 입력을 포함하되(대소문자 무시) 입력과 똑같은 항목(대소문자 구분)은 뺀다 — 방금 제출한 검색어를 다시 제안하지 않으려는 것.
internal fun resolveRecentSearchesPane(
    searches: List<String>,
    query: String,
    hasGenreFilter: Boolean
): RecentSearchesPane {
    val q = query.trim()
    if (q.isNotEmpty()) {
        val filtered = searches.filter { it.contains(q, ignoreCase = true) && it != q }
        return if (filtered.isNotEmpty()) RecentSearchesPane.Filtered(filtered) else RecentSearchesPane.Hidden
    }
    return when {
        hasGenreFilter -> RecentSearchesPane.KeepVisibility(searches)
        searches.isNotEmpty() -> RecentSearchesPane.Recent(searches)
        else -> RecentSearchesPane.Initial
    }
}
