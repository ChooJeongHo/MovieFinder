package com.choo.moviefinder.domain.repository

import androidx.paging.PagingData
import com.choo.moviefinder.domain.model.Genre
import com.choo.moviefinder.domain.model.Movie
import kotlinx.coroutines.flow.Flow

interface MovieQueryRepository {

    // 현재 상영 중인 영화 목록을 페이징 데이터로 반환한다
    fun getNowPlayingMovies(): Flow<PagingData<Movie>>

    // 인기 영화 목록을 페이징 데이터로 반환한다
    fun getPopularMovies(): Flow<PagingData<Movie>>

    // 일별 트렌딩 영화 목록을 페이징 데이터로 반환한다
    fun getTrendingMovies(): Flow<PagingData<Movie>>

    // 개봉 예정 영화 목록을 페이징 데이터로 반환한다
    fun getUpcomingMovies(): Flow<PagingData<Movie>>

    // 검색어와 연도 필터로 영화를 검색하여 페이징 데이터로 반환한다
    fun searchMovies(query: String, year: Int? = null): Flow<PagingData<Movie>>

    // 장르, 정렬, 연도 필터로 영화를 탐색하여 페이징 데이터로 반환한다
    fun discoverMovies(
        genres: Set<Int> = emptySet(),
        sortBy: String = "popularity.desc",
        year: Int? = null
    ): Flow<PagingData<Movie>>

    // 영화 장르 목록을 조회한다
    suspend fun getGenreList(): List<Genre>
}
