package com.choo.moviefinder.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.toDomain
import com.choo.moviefinder.domain.model.Movie

class DiscoverPagingSource(
    private val apiService: MovieApiService,
    private val genres: String?,
    private val sortBy: String,
    private val year: Int?
) : PagingSource<Int, Movie>() {

    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val page = params.key ?: 1
        return try {
            val response = apiService.discoverMovies(
                page = page,
                withGenres = genres,
                sortBy = sortBy,
                year = year
            )
            LoadResult.Page(
                data = response.results.map { it.toDomain() },
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (page >= response.totalPages) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
