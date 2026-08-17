package com.choo.moviefinder.data.remote.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface KmrbApiService {

    // 영화 등급분류정보 검색 (XML 응답, serviceKey는 KmrbOkHttpClient 인터셉터가 자동 주입)
    @GET("movie_v3/movie_search_v3")
    suspend fun searchMovieRating(
        @Query("title") title: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 10
    ): ResponseBody
}
