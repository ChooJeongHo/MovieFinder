package com.choo.moviefinder.data.paging

import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.MovieListResponse

class MoviePagingSource(
    private val apiService: MovieApiService,
    private val query: String,
    private val year: Int? = null
) : BaseMoviePagingSource() {

    // 검색 API를 호출하여 페이지 단위로 영화 목록 조회
    override suspend fun fetchPage(page: Int): MovieListResponse =
        apiService.searchMovies(query, page, year = year)
}
