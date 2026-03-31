package com.choo.moviefinder.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.toDomain
import com.choo.moviefinder.domain.model.Movie

class TrendingPagingSource(
    private val apiService: MovieApiService
) : PagingSource<Int, Movie>() {

    // 새로고침 시 기준이 되는 페이지 키 계산
    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    // 트렌딩 영화 API를 호출하여 페이지 단위로 영화 목록 로드
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val page = params.key ?: 1
        return try {
            val response = apiService.getTrendingMovies(page)
            LoadResult.Page(
                data = response.results.map { it.toDomain() },
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (response.results.size < params.loadSize) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
