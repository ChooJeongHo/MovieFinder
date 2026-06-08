package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.remote.api.TmdbAuthApiService
import com.choo.moviefinder.data.remote.api.TmdbV3SessionApiService
import com.choo.moviefinder.data.remote.dto.RatingRequest
import com.choo.moviefinder.data.remote.dto.toDomain
import com.choo.moviefinder.data.util.safeApiCall
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.TmdbAuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbAuthRepositoryImpl @Inject constructor(
    private val authApiService: TmdbAuthApiService,
    private val sessionApiService: TmdbV3SessionApiService
) : TmdbAuthRepository {

    override suspend fun getRequestToken(): String =
        safeApiCall { authApiService.getRequestToken().requestToken }

    override suspend fun exchangeAccessToken(requestToken: String): Triple<String, String, String> =
        safeApiCall {
            val accessTokenResponse = authApiService.exchangeAccessToken(
                mapOf("request_token" to requestToken)
            )
            val sessionResponse = sessionApiService.convertToV3Session(
                mapOf("access_token" to accessTokenResponse.accessToken)
            )
            Triple(accessTokenResponse.accessToken, accessTokenResponse.accountId, sessionResponse.sessionId)
        }

    override suspend fun revokeAccessToken(accessToken: String) {
        safeApiCall { authApiService.revokeAccessToken(mapOf("access_token" to accessToken)) }
    }

    override suspend fun getAccountFavorites(accountId: String, bearer: String): List<Movie> =
        safeApiCall { authApiService.getAccountFavorites(accountId, bearer).results.map { it.toDomain() } }

    override suspend fun getAccountWatchlist(accountId: String, bearer: String): List<Movie> =
        safeApiCall { authApiService.getAccountWatchlist(accountId, bearer).results.map { it.toDomain() } }

    override suspend fun rateMovie(movieId: Int, sessionId: String, rating: Float): Boolean =
        safeApiCall { sessionApiService.rateMovie(movieId, sessionId, RatingRequest(rating)).success }
}
