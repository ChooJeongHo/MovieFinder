package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.Movie

interface TmdbAuthRepository {

    // v4 요청 토큰을 발급하고 토큰 문자열을 반환한다
    suspend fun getRequestToken(): String

    // 승인된 요청 토큰을 액세스 토큰으로 교환하고 Triple(accessToken, accountId, sessionId)을 반환한다
    suspend fun exchangeAccessToken(requestToken: String): Triple<String, String, String>

    // v4 액세스 토큰을 폐기한다 (로그아웃)
    suspend fun revokeAccessToken(accessToken: String)

    // TMDB 계정 즐겨찾기 목록을 조회한다
    suspend fun getAccountFavorites(accountId: String, bearer: String): List<Movie>

    // TMDB 계정 워치리스트 목록을 조회한다
    suspend fun getAccountWatchlist(accountId: String, bearer: String): List<Movie>

    // v3 세션 ID로 영화 평점을 제출하고 성공 여부를 반환한다
    suspend fun rateMovie(movieId: Int, sessionId: String, rating: Float): Boolean
}
