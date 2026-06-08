package com.choo.moviefinder.domain.repository

import kotlinx.coroutines.flow.Flow

interface TokenRepository {

    // TMDB 액세스 토큰을 Flow로 반환한다
    fun getAccessToken(): Flow<String?>

    // TMDB 인증 정보(액세스 토큰, 계정 ID, 세션 ID)를 저장한다
    suspend fun saveTokens(accessToken: String, accountId: String, sessionId: String)

    // TMDB 인증 정보를 삭제한다
    suspend fun clearTokens()

    // TMDB 세션 ID를 일회성으로 조회한다
    suspend fun getSessionIdOnce(): String?

    // TMDB 액세스 토큰과 계정 ID를 일회성으로 조회한다 (Pair<accessToken, accountId>)
    suspend fun getAuthOnce(): Pair<String?, String?>
}
