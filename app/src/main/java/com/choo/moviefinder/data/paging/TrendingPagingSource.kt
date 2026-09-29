package com.choo.moviefinder.data.paging

import com.choo.moviefinder.core.util.AppLanguageProvider
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.MovieListResponse

class TrendingPagingSource(
    private val apiService: MovieApiService,
    private val appLanguageProvider: AppLanguageProvider
) : BaseMoviePagingSource() {

    // 트렌딩 영화 API를 호출하여 페이지 단위로 영화 목록 조회 (앱 언어를 따름)
    override suspend fun fetchPage(page: Int): MovieListResponse =
        apiService.getTrendingMovies(page, appLanguageProvider.currentApiLanguage())
}
