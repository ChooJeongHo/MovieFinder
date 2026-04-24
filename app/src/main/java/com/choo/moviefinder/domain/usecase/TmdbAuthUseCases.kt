package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.data.remote.api.TmdbAuthApiService
import com.choo.moviefinder.data.remote.api.TmdbV3SessionApiService
import com.choo.moviefinder.data.remote.dto.RatingRequest
import com.choo.moviefinder.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetTmdbAccessTokenUseCase @Inject constructor(
    private val repository: PreferencesRepository
) {
    operator fun invoke(): Flow<String?> = repository.getTmdbAccessToken()
}

@Singleton
class GetTmdbRequestTokenUseCase @Inject constructor(
    private val authApiService: TmdbAuthApiService
) {
    // v4 요청 토큰을 발급하고 토큰 문자열을 반환한다
    suspend operator fun invoke(): String = authApiService.getRequestToken().requestToken
}

@Singleton
class ExchangeTmdbTokenUseCase @Inject constructor(
    private val authApiService: TmdbAuthApiService,
    private val sessionApiService: TmdbV3SessionApiService,
    private val repository: PreferencesRepository
) {
    // 승인된 요청 토큰을 액세스 토큰으로 교환하고 v3 세션 ID까지 함께 저장한다
    suspend operator fun invoke(requestToken: String) {
        val accessTokenResponse = authApiService.exchangeAccessToken(
            mapOf("request_token" to requestToken)
        )
        val sessionResponse = sessionApiService.convertToV3Session(
            mapOf("access_token" to accessTokenResponse.accessToken)
        )
        repository.saveTmdbAuth(
            accessToken = accessTokenResponse.accessToken,
            accountId = accessTokenResponse.accountId,
            sessionId = sessionResponse.sessionId
        )
    }
}

@Singleton
class RevokeTmdbAuthUseCase @Inject constructor(
    private val authApiService: TmdbAuthApiService,
    private val repository: PreferencesRepository
) {
    // 액세스 토큰을 폐기하고 로컬 인증 정보를 삭제한다
    suspend operator fun invoke(accessToken: String) {
        try {
            authApiService.revokeAccessToken(mapOf("access_token" to accessToken))
        } catch (_: Exception) { }
        repository.clearTmdbAuth()
    }
}

@Singleton
class SubmitTmdbRatingUseCase @Inject constructor(
    private val sessionApiService: TmdbV3SessionApiService,
    private val repository: PreferencesRepository
) {
    // v3 세션 ID로 영화 평점을 TMDB에 제출하고 성공 여부를 반환한다
    suspend operator fun invoke(movieId: Int, rating: Float): Boolean {
        val sessionId = repository.getTmdbSessionIdOnce() ?: return false
        val response = sessionApiService.rateMovie(movieId, sessionId, RatingRequest(rating))
        return response.success
    }
}
