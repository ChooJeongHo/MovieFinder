package com.choo.moviefinder.domain.repository

import androidx.paging.PagingData
import com.choo.moviefinder.domain.model.Cast
import com.choo.moviefinder.domain.model.Genre
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.MovieDetail
import com.choo.moviefinder.domain.model.Review
import kotlinx.coroutines.flow.Flow

interface MovieRepository {

    // 현재 상영 중인 영화 목록을 페이징 데이터로 반환한다
    fun getNowPlayingMovies(): Flow<PagingData<Movie>>

    // 인기 영화 목록을 페이징 데이터로 반환한다
    fun getPopularMovies(): Flow<PagingData<Movie>>

    // 일별 트렌딩 영화 목록을 페이징 데이터로 반환한다
    fun getTrendingMovies(): Flow<PagingData<Movie>>

    // 검색어와 연도 필터로 영화를 검색하여 페이징 데이터로 반환한다
    fun searchMovies(query: String, year: Int? = null): Flow<PagingData<Movie>>

    // 장르, 정렬, 연도 필터로 영화를 탐색하여 페이징 데이터로 반환한다
    fun discoverMovies(
        genres: Set<Int> = emptySet(),
        sortBy: String = "popularity.desc",
        year: Int? = null
    ): Flow<PagingData<Movie>>

    // 영화 ID로 상세 정보를 조회한다
    suspend fun getMovieDetail(movieId: Int): MovieDetail

    // 영화 ID로 출연진 목록을 조회한다
    suspend fun getMovieCredits(movieId: Int): List<Cast>

    // 영화 ID로 비슷한 영화 목록을 조회한다
    suspend fun getSimilarMovies(movieId: Int): List<Movie>

    // 영화 ID로 추천 영화 목록을 조회한다
    suspend fun getMovieRecommendations(movieId: Int): List<Movie>

    // 영화 ID로 YouTube 예고편 키를 조회한다
    suspend fun getMovieTrailerKey(movieId: Int): String?

    // 영화 ID로 콘텐츠 등급 정보를 조회한다
    suspend fun getMovieCertification(movieId: Int): String?

    // 영화 ID로 사용자 리뷰 목록을 조회한다
    suspend fun getMovieReviews(movieId: Int): List<Review>

    // 영화 장르 목록을 조회한다
    suspend fun getGenreList(): List<Genre>
}
