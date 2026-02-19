package com.choo.moviefinder.data.remote.api

import com.choo.moviefinder.data.remote.dto.CreditsResponse
import com.choo.moviefinder.data.remote.dto.MovieDetailDto
import com.choo.moviefinder.data.remote.dto.MovieListResponse
import com.choo.moviefinder.data.remote.dto.VideoResponse
import com.choo.moviefinder.data.util.Constants
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieApiService {

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("page") page: Int,
        @Query("language") language: String = Constants.LANGUAGE_KO
    ): MovieListResponse

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("page") page: Int,
        @Query("language") language: String = Constants.LANGUAGE_KO
    ): MovieListResponse

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("language") language: String = Constants.LANGUAGE_KO,
        @Query("year") year: Int? = null
    ): MovieListResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = Constants.LANGUAGE_KO
    ): MovieDetailDto

    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = Constants.LANGUAGE_KO
    ): CreditsResponse

    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = Constants.LANGUAGE_EN
    ): VideoResponse

    @GET("movie/{movie_id}/similar")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("page") page: Int = 1,
        @Query("language") language: String = Constants.LANGUAGE_KO
    ): MovieListResponse
}