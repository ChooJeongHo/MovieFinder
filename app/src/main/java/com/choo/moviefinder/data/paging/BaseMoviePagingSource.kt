package com.choo.moviefinder.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.choo.moviefinder.data.remote.dto.MovieListResponse
import com.choo.moviefinder.data.remote.dto.toDomain
import com.choo.moviefinder.domain.model.Movie
import kotlinx.coroutines.CancellationException

// 페이지 키 계산과 공통 로드/에러 처리 로직을 담당하는 PagingSource 베이스 클래스
abstract class BaseMoviePagingSource : PagingSource<Int, Movie>() {

    // 각 API 엔드포인트별 페이지 단위 영화 목록 조회
    protected abstract suspend fun fetchPage(page: Int): MovieListResponse

    // 새로고침 시 기준이 되는 페이지 키 계산
    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    // API를 호출하여 페이지 단위로 영화 목록 로드
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val page = params.key ?: STARTING_PAGE
        return try {
            val response = fetchPage(page)
            LoadResult.Page(
                data = response.results.map { it.toDomain() },
                prevKey = if (page == STARTING_PAGE) null else page - 1,
                nextKey = if (page >= response.totalPages) null else page + 1
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            LoadResult.Error(e)
        }
    }

    private companion object {
        const val STARTING_PAGE = 1
    }
}
