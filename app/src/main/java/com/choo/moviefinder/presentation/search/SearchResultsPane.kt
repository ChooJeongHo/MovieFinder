package com.choo.moviefinder.presentation.search

import androidx.paging.LoadState

// 검색 결과 영역에 무엇을 보여줄지 결정한다. SearchFragment.handleLoadStates가 이 결과로 뷰 가시성만 바꾼다.
internal sealed interface SearchResultsPane {
    data object Loading : SearchResultsPane
    data object Results : SearchResultsPane
    data object NoResults : SearchResultsPane
    data class Error(val cause: Throwable) : SearchResultsPane
}

// "검색 결과 없음"은 더 가져올 페이지가 없다는 게 확정된 뒤(append.endOfPaginationReached)에만 띄운다.
// 등급 필터는 PagingData.filter로 페이지 단위 후처리라서, 한 페이지가 통째로 걸러지면 refresh는 이미 NotLoading인데
// itemCount는 0이다. 이때 Paging은 초기 힌트(뒤쪽 표시 항목 0 < prefetchDistance)로 다음 페이지를 스스로 이어서
// 로드하므로 이 상태는 "결과 없음"이 아니라 "찾는 중"이다 (PagingEmptyStateRegressionTest가 이 전제를 고정한다).
// append가 실패해 멈춘 경우엔 빈 화면 대신 오류 화면으로 재시도할 수 있게 한다.
internal fun resolveSearchResultsPane(refresh: LoadState, append: LoadState, itemCount: Int): SearchResultsPane =
    when {
        refresh is LoadState.Loading -> SearchResultsPane.Loading
        refresh is LoadState.Error -> SearchResultsPane.Error(refresh.error)
        itemCount > 0 -> SearchResultsPane.Results
        append is LoadState.Error -> SearchResultsPane.Error(append.error)
        append is LoadState.NotLoading && append.endOfPaginationReached -> SearchResultsPane.NoResults
        else -> SearchResultsPane.Loading
    }
