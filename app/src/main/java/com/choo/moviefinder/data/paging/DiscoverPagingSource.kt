package com.choo.moviefinder.data.paging

import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.MovieListResponse

class DiscoverPagingSource(
    private val apiService: MovieApiService,
    private val genres: String?,
    private val sortBy: String,
    private val year: Int?
) : BaseMoviePagingSource() {

    // Discover API를 호출하여 장르/정렬 기반으로 영화 목록 조회
    override suspend fun fetchPage(page: Int): MovieListResponse =
        apiService.discoverMovies(
            page = page,
            withGenres = genres,
            sortBy = sortBy,
            year = year
        )
}
