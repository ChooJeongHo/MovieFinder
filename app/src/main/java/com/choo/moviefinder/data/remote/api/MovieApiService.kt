package com.choo.moviefinder.data.remote.api

import com.choo.moviefinder.data.remote.dto.CollectionDto
import com.choo.moviefinder.data.remote.dto.CreditsResponse
import com.choo.moviefinder.data.remote.dto.GenreListResponse
import com.choo.moviefinder.data.remote.dto.MovieDetailDto
import com.choo.moviefinder.data.remote.dto.MovieListResponse
import com.choo.moviefinder.data.remote.dto.PersonCreditsResponse
import com.choo.moviefinder.data.remote.dto.PersonDetailDto
import com.choo.moviefinder.data.remote.dto.PersonSearchResponse
import com.choo.moviefinder.data.remote.dto.ReleaseDateResponse
import com.choo.moviefinder.data.remote.dto.ReviewResponse
import com.choo.moviefinder.data.remote.dto.VideoResponse
import com.choo.moviefinder.data.util.Constants
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Suppress("TooManyFunctions")
interface MovieApiService {

    // 현재 상영 중인 영화 목록 조회
    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("page") page: Int,
        @Query("language") language: String = Constants.LANGUAGE_KO
    ): MovieListResponse

    // 인기 영화 목록 조회
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("page") page: Int,
        @Query("language") language: String = Constants.LANGUAGE_KO
    ): MovieListResponse

    // 검색어로 영화 검색 (연도 필터 선택)
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("language") language: String = Constants.LANGUAGE_KO,
        @Query("year") year: Int? = null
    ): MovieListResponse

    // 영화 상세 정보 조회
    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = Constants.LANGUAGE_KO
    ): MovieDetailDto

    // 영화 출연진 정보 조회
    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = Constants.LANGUAGE_KO
    ): CreditsResponse

    // 영화 관련 영상 (예고편 등) 조회
    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = Constants.LANGUAGE_EN
    ): VideoResponse

    // 비슷한 영화 목록 조회
    @GET("movie/{movie_id}/similar")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("page") page: Int = 1,
        @Query("language") language: String = Constants.LANGUAGE_KO
    ): MovieListResponse

    // 영화 사용자 리뷰 조회
    @GET("movie/{movie_id}/reviews")
    suspend fun getMovieReviews(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = Constants.LANGUAGE_EN
    ): ReviewResponse

    // 영화 개봉일 및 콘텐츠 등급 정보 조회
    @GET("movie/{movie_id}/release_dates")
    suspend fun getMovieReleaseDates(
        @Path("movie_id") movieId: Int
    ): ReleaseDateResponse

    // 영화 장르 목록 조회
    @GET("genre/movie/list")
    suspend fun getGenreList(
        @Query("language") language: String = Constants.LANGUAGE_KO
    ): GenreListResponse

    // 일별 트렌딩 영화 목록 조회
    @GET("trending/movie/day")
    suspend fun getTrendingMovies(
        @Query("page") page: Int = 1,
        @Query("language") language: String = Constants.LANGUAGE_KO
    ): MovieListResponse

    // 개봉 예정 영화 목록 조회
    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("page") page: Int,
        @Query("language") language: String = Constants.LANGUAGE_KO,
        @Query("region") region: String = "KR"
    ): MovieListResponse

    // 장르/정렬/연도 기반 영화 탐색
    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("page") page: Int,
        @Query("with_genres") withGenres: String? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("year") year: Int? = null,
        @Query("language") language: String = Constants.LANGUAGE_KO
    ): MovieListResponse

    // 추천 영화 목록 조회
    @GET("movie/{movie_id}/recommendations")
    suspend fun getMovieRecommendations(
        @Path("movie_id") movieId: Int,
        @Query("page") page: Int = 1,
        @Query("language") language: String = Constants.LANGUAGE_KO
    ): MovieListResponse

    // 배우/감독 상세 정보 조회
    @GET("person/{person_id}")
    suspend fun getPersonDetail(
        @Path("person_id") personId: Int,
        @Query("language") language: String = Constants.LANGUAGE_KO
    ): PersonDetailDto

    // 배우/감독 출연 영화 크레딧 조회
    @GET("person/{person_id}/movie_credits")
    suspend fun getPersonMovieCredits(
        @Path("person_id") personId: Int,
        @Query("language") language: String = Constants.LANGUAGE_KO
    ): PersonCreditsResponse

    // 배우/인물 이름으로 검색
    @GET("search/person")
    suspend fun searchPerson(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = Constants.LANGUAGE_KO
    ): PersonSearchResponse

    // 영화 컬렉션 정보 조회
    @GET("collection/{collection_id}")
    suspend fun getCollection(
        @Path("collection_id") collectionId: Int,
        @Query("language") language: String = Constants.LANGUAGE_KO
    ): CollectionDto
}