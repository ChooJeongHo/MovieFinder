package com.choo.moviefinder.data.local

import com.choo.moviefinder.domain.repository.TokenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRepositoryImpl @Inject constructor(
    private val secureTokenStore: SecureTokenStore
) : TokenRepository {

    private val _accessToken = MutableStateFlow(secureTokenStore.getAccessToken())

    override fun getAccessToken(): Flow<String?> = _accessToken.asStateFlow()

    override suspend fun saveTokens(accessToken: String, accountId: String, sessionId: String) {
        secureTokenStore.saveTokens(accessToken, accountId, sessionId)
        _accessToken.value = accessToken
    }

    override suspend fun clearTokens() {
        secureTokenStore.clearTokens()
        _accessToken.value = null
    }

    override suspend fun getSessionIdOnce(): String? = secureTokenStore.getSessionId()

    override suspend fun getAuthOnce(): Pair<String?, String?> =
        Pair(secureTokenStore.getAccessToken(), secureTokenStore.getAccountId())
}
