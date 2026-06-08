package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.remote.api.TmdbAuthApiService
import com.choo.moviefinder.data.remote.api.TmdbV3SessionApiService
import com.choo.moviefinder.data.remote.dto.AccessTokenResponse
import com.choo.moviefinder.data.remote.dto.RequestTokenResponse
import com.choo.moviefinder.data.remote.dto.RatingResponse
import com.choo.moviefinder.data.remote.dto.SessionResponse
import com.choo.moviefinder.domain.model.DomainException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TmdbAuthRepositoryImplTest {

    private lateinit var authApiService: TmdbAuthApiService
    private lateinit var sessionApiService: TmdbV3SessionApiService
    private lateinit var repository: TmdbAuthRepositoryImpl

    @Before
    fun setUp() {
        authApiService = mockk()
        sessionApiService = mockk()
        repository = TmdbAuthRepositoryImpl(authApiService, sessionApiService)
    }

    @Test
    fun `getRequestToken returns token from api`() = runTest {
        coEvery { authApiService.getRequestToken() } returns
            RequestTokenResponse(requestToken = "req_abc", success = true)

        val result = repository.getRequestToken()

        assertEquals("req_abc", result)
    }

    @Test
    fun `getRequestToken wraps api error in DomainException`() = runTest {
        coEvery { authApiService.getRequestToken() } throws java.io.IOException("network error")

        val thrown = runCatching { repository.getRequestToken() }.exceptionOrNull()

        assertTrue(thrown is DomainException.NetworkError)
    }

    @Test
    fun `exchangeAccessToken returns correct triple`() = runTest {
        coEvery { authApiService.exchangeAccessToken(any()) } returns
            AccessTokenResponse(accessToken = "tok", accountId = "acct", success = true)
        coEvery { sessionApiService.convertToV3Session(any()) } returns
            SessionResponse(sessionId = "sess", success = true)

        val result = repository.exchangeAccessToken("req_token")

        assertEquals(Triple("tok", "acct", "sess"), result)
    }

    @Test
    fun `exchangeAccessToken passes request token correctly`() = runTest {
        coEvery { authApiService.exchangeAccessToken(mapOf("request_token" to "req_xyz")) } returns
            AccessTokenResponse(accessToken = "tok", accountId = "acct", success = true)
        coEvery { sessionApiService.convertToV3Session(any()) } returns
            SessionResponse(sessionId = "sess", success = true)

        repository.exchangeAccessToken("req_xyz")

        coVerify(exactly = 1) { authApiService.exchangeAccessToken(mapOf("request_token" to "req_xyz")) }
    }

    @Test
    fun `revokeAccessToken calls api with correct token`() = runTest {
        coEvery { authApiService.revokeAccessToken(any()) } returns mockk(relaxed = true)

        repository.revokeAccessToken("tok_to_revoke")

        coVerify(exactly = 1) { authApiService.revokeAccessToken(mapOf("access_token" to "tok_to_revoke")) }
    }

    @Test
    fun `getAccountFavorites returns mapped movies`() = runTest {
        val movieDto = mockk<com.choo.moviefinder.data.remote.dto.MovieDto>(relaxed = true)
        coEvery { authApiService.getAccountFavorites("acct", "Bearer tok") } returns
            mockk { coEvery { results } returns listOf(movieDto) }

        repository.getAccountFavorites("acct", "Bearer tok")

        coVerify(exactly = 1) { authApiService.getAccountFavorites("acct", "Bearer tok") }
    }

    @Test
    fun `getAccountWatchlist delegates to api`() = runTest {
        coEvery { authApiService.getAccountWatchlist("acct", "Bearer tok") } returns
            mockk { coEvery { results } returns emptyList() }

        val result = repository.getAccountWatchlist("acct", "Bearer tok")

        assertTrue(result.isEmpty())
        coVerify(exactly = 1) { authApiService.getAccountWatchlist("acct", "Bearer tok") }
    }

    @Test
    fun `rateMovie returns true on success`() = runTest {
        coEvery { sessionApiService.rateMovie(99, "sess", any()) } returns
            RatingResponse(success = true)

        val result = repository.rateMovie(movieId = 99, sessionId = "sess", rating = 4.0f)

        assertTrue(result)
    }

    @Test
    fun `rateMovie wraps network error in DomainException`() = runTest {
        coEvery { sessionApiService.rateMovie(any(), any(), any()) } throws
            java.net.SocketTimeoutException("timeout")

        val thrown = runCatching {
            repository.rateMovie(movieId = 1, sessionId = "sess", rating = 3.0f)
        }.exceptionOrNull()

        assertTrue(thrown is DomainException.Timeout)
    }
}
