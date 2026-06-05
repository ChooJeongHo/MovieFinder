package com.choo.moviefinder.data.paging

import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.MovieListResponse

class UpcomingPagingSource(
    private val apiService: MovieApiService
) : BaseMoviePagingSource() {

    // 개봉 예정 영화 API를 호출하여 페이지 단위로 영화 목록 조회
    override suspend fun fetchPage(page: Int): MovieListResponse =
        apiService.getUpcomingMovies(page)
}
