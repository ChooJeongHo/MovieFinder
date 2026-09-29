package com.choo.moviefinder.data.paging

import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.MovieListResponse

// 검색 결과는 앱 언어와 무관하게 항상 ko-KR로 고정한다 (KMRB 등급 필터가 movie.title로 직접 매칭하므로,
// FilterMoviesByKoreanRatingUseCase 참고 — 앱 언어를 따르게 했더니 필터 초기 지연이 과도해 되돌렸다).
class MoviePagingSource(
    private val apiService: MovieApiService,
    private val query: String,
    private val year: Int? = null
) : BaseMoviePagingSource() {

    // 검색 API를 호출하여 페이지 단위로 영화 목록 조회
    override suspend fun fetchPage(page: Int): MovieListResponse =
        apiService.searchMovies(query, page, year = year)
}
