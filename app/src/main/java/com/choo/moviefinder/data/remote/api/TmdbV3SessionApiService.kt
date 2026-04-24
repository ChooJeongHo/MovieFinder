package com.choo.moviefinder.data.remote.api

import com.choo.moviefinder.data.remote.dto.RatingRequest
import com.choo.moviefinder.data.remote.dto.RatingResponse
import com.choo.moviefinder.data.remote.dto.SessionResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbV3SessionApiService {

    // v4 액세스 토큰을 v3 세션 ID로 변환
    @POST("authentication/session/convert/4")
    suspend fun convertToV3Session(@Body body: Map<String, String>): SessionResponse

    // 영화 평점 제출 (v3 세션 ID 필요)
    @POST("movie/{movie_id}/rating")
    suspend fun rateMovie(
        @Path("movie_id") movieId: Int,
        @Query("session_id") sessionId: String,
        @Body body: RatingRequest
    ): RatingResponse

    // 영화 평점 삭제
    @DELETE("movie/{movie_id}/rating")
    suspend fun deleteMovieRating(
        @Path("movie_id") movieId: Int,
        @Query("session_id") sessionId: String
    ): RatingResponse
}
