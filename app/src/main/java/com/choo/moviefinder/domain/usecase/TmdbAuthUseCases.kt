package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.PreferencesRepository
import com.choo.moviefinder.domain.repository.TmdbAuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetTmdbAccessTokenUseCase @Inject constructor(
    private val repository: PreferencesRepository
) {
    operator fun invoke(): Flow<String?> = repository.getTmdbAccessToken()
}

@Reusable
class GetTmdbRequestTokenUseCase @Inject constructor(
    private val tmdbAuthRepository: TmdbAuthRepository
) {
    // v4 요청 토큰을 발급하고 토큰 문자열을 반환한다
    suspend operator fun invoke(): String = tmdbAuthRepository.getRequestToken()
}

@Reusable
class ExchangeTmdbTokenUseCase @Inject constructor(
    private val tmdbAuthRepository: TmdbAuthRepository,
    private val repository: PreferencesRepository
) {
    // 승인된 요청 토큰을 액세스 토큰으로 교환하고 v3 세션 ID까지 함께 저장한다
    suspend operator fun invoke(requestToken: String) {
        val (accessToken, accountId, sessionId) = tmdbAuthRepository.exchangeAccessToken(requestToken)
        repository.saveTmdbAuth(
            accessToken = accessToken,
            accountId = accountId,
            sessionId = sessionId
        )
    }
}

@Reusable
class RevokeTmdbAuthUseCase @Inject constructor(
    private val tmdbAuthRepository: TmdbAuthRepository,
    private val repository: PreferencesRepository
) {
    // 액세스 토큰을 폐기하고 로컬 인증 정보를 삭제한다
    suspend operator fun invoke(accessToken: String) {
        try {
            tmdbAuthRepository.revokeAccessToken(accessToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "TMDB 액세스 토큰 폐기 실패 — 로컬 인증 정보는 삭제합니다")
        }
        repository.clearTmdbAuth()
    }
}

@Reusable
class SubmitTmdbRatingUseCase @Inject constructor(
    private val tmdbAuthRepository: TmdbAuthRepository,
    private val repository: PreferencesRepository
) {
    // v3 세션 ID로 영화 평점을 TMDB에 제출하고 성공 여부를 반환한다
    suspend operator fun invoke(movieId: Int, rating: Float): Boolean {
        val sessionId = repository.getTmdbSessionIdOnce() ?: return false
        return tmdbAuthRepository.rateMovie(movieId, sessionId, rating)
    }
}
