package com.choo.moviefinder.data.remote.api

import com.choo.moviefinder.data.remote.dto.AccessTokenResponse
import com.choo.moviefinder.data.remote.dto.MovieListResponse
import com.choo.moviefinder.data.remote.dto.RequestTokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbAuthApiService {

    // v4 요청 토큰 발급
    @POST("auth/request_token")
    suspend fun getRequestToken(): RequestTokenResponse

    // v4 요청 토큰을 사용자 액세스 토큰으로 교환
    @POST("auth/access_token")
    suspend fun exchangeAccessToken(@Body body: Map<String, String>): AccessTokenResponse

    // v4 액세스 토큰 폐기 (로그아웃)
    @DELETE("auth/access_token")
    suspend fun revokeAccessToken(@Body body: Map<String, String>): Response<Unit>

    // TMDB 계정 즐겨찾기 목록 조회 (v4)
    @GET("account/{account_id}/movie/favorites")
    suspend fun getAccountFavorites(
        @Path("account_id") accountId: String,
        @Header("Authorization") bearerToken: String,
        @Query("page") page: Int = 1
    ): MovieListResponse

    // TMDB 계정 워치리스트 목록 조회 (v4)
    @GET("account/{account_id}/movie/watchlist")
    suspend fun getAccountWatchlist(
        @Path("account_id") accountId: String,
        @Header("Authorization") bearerToken: String,
        @Query("page") page: Int = 1
    ): MovieListResponse
}
