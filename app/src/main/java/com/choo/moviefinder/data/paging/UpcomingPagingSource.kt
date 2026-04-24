package com.choo.moviefinder.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.toDomain
import com.choo.moviefinder.domain.model.Movie
import kotlinx.coroutines.CancellationException

class UpcomingPagingSource(
    private val apiService: MovieApiService
) : PagingSource<Int, Movie>() {

    // 새로고침 시 기준이 되는 페이지 키 계산
    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    // 개봉 예정 영화 API를 호출하여 페이지 단위로 영화 목록 로드
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val page = params.key ?: 1
        return try {
            val response = apiService.getUpcomingMovies(page)
            LoadResult.Page(
                data = response.results.map { it.toDomain() },
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (page >= response.totalPages) null else page + 1
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            LoadResult.Error(e)
        }
    }
}
