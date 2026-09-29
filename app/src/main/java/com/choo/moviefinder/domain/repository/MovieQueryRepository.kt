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

    // 검색어로 영화를 1페이지만 즉시 조회한다 (박스오피스-TMDB 매칭 등 단발성 조회용)
    suspend fun searchMoviesOnce(query: String): List<Movie>

    // 로컬에 캐싱된 현재 상영작/인기 영화 전체를 네트워크 호출 없이 스냅샷으로 조회한다
    // (박스오피스-TMDB 매칭 시 로컬 우선 조회로 반복적인 네트워크 검색을 피하기 위한 용도)
    suspend fun getCachedMoviesSnapshot(): List<Movie>

    // 앱 언어 변경 시 현재 상영작/인기 영화 로컬 캐시를 무효화한다 (이전 언어의 제목/줄거리가
    // 최대 1시간 TTL 동안 남아있는 것을 방지)
    suspend fun invalidateHomeMovieCache()

    // 장르, 정렬, 연도 필터로 영화를 탐색하여 페이징 데이터로 반환한다
    fun discoverMovies(
        genres: Set<Int> = emptySet(),
        sortBy: String = "popularity.desc",
        year: Int? = null
    ): Flow<PagingData<Movie>>

    // 영화 장르 목록을 조회한다
    suspend fun getGenreList(): List<Genre>
}
