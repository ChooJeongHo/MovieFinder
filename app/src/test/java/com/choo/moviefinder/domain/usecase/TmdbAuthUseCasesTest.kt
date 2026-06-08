package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.TmdbAuthRepository
import com.choo.moviefinder.domain.repository.TokenRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TmdbAuthUseCasesTest {

    private lateinit var tmdbAuthRepository: TmdbAuthRepository
    private lateinit var tokenRepository: TokenRepository

    @Before
    fun setUp() {
        tmdbAuthRepository = mockk()
        tokenRepository = mockk(relaxUnitFun = true)
    }

    // --- GetTmdbAccessTokenUseCase ---

    @Test
    fun `GetTmdbAccessTokenUseCase delegates to TokenRepository`() {
        val flow = flowOf("access_token_123")
        every { tokenRepository.getAccessToken() } returns flow
        val useCase = GetTmdbAccessTokenUseCase(tokenRepository)

        val result = useCase()

        verify(exactly = 1) { tokenRepository.getAccessToken() }
        assertEquals(flow, result)
    }

    @Test
    fun `GetTmdbAccessTokenUseCase returns null-emitting flow when no token stored`() {
        every { tokenRepository.getAccessToken() } returns flowOf(null)
        val useCase = GetTmdbAccessTokenUseCase(tokenRepository)

        val result = useCase()

        assertEquals(flowOf<String?>(null).javaClass, result.javaClass)
        verify(exactly = 1) { tokenRepository.getAccessToken() }
    }

    // --- GetTmdbRequestTokenUseCase ---

    @Test
    fun `GetTmdbRequestTokenUseCase delegates to TmdbAuthRepository`() = runTest {
        coEvery { tmdbAuthRepository.getRequestToken() } returns "req_token_abc"
        val useCase = GetTmdbRequestTokenUseCase(tmdbAuthRepository)

        val result = useCase()

        assertEquals("req_token_abc", result)
        coVerify(exactly = 1) { tmdbAuthRepository.getRequestToken() }
    }

    // --- ExchangeTmdbTokenUseCase ---

    @Test
    fun `ExchangeTmdbTokenUseCase exchanges token and saves auth to TokenRepository`() = runTest {
        val triple = Triple("access_token", "account_id", "session_id")
        coEvery { tmdbAuthRepository.exchangeAccessToken("req_token") } returns triple
        val useCase = ExchangeTmdbTokenUseCase(tmdbAuthRepository, tokenRepository)

        useCase("req_token")

        coVerify(exactly = 1) { tmdbAuthRepository.exchangeAccessToken("req_token") }
        coVerify(exactly = 1) {
            tokenRepository.saveTokens(
                accessToken = "access_token",
                accountId = "account_id",
                sessionId = "session_id"
            )
        }
    }

    @Test
    fun `ExchangeTmdbTokenUseCase passes exact Triple values from repository to saveTokens`() = runTest {
        val triple = Triple("tok123", "acct456", "sess789")
        coEvery { tmdbAuthRepository.exchangeAccessToken(any()) } returns triple
        val useCase = ExchangeTmdbTokenUseCase(tmdbAuthRepository, tokenRepository)

        useCase("any_token")

        coVerify(exactly = 1) {
            tokenRepository.saveTokens("tok123", "acct456", "sess789")
        }
    }

    // --- RevokeTmdbAuthUseCase ---

    @Test
    fun `RevokeTmdbAuthUseCase revokes token and clears local auth`() = runTest {
        coEvery { tmdbAuthRepository.revokeAccessToken("tok") } returns Unit
        val useCase = RevokeTmdbAuthUseCase(tmdbAuthRepository, tokenRepository)

        useCase("tok")

        coVerify(exactly = 1) { tmdbAuthRepository.revokeAccessToken("tok") }
        coVerify(exactly = 1) { tokenRepository.clearTokens() }
    }

    @Test
    fun `RevokeTmdbAuthUseCase clears local auth even when revokeAccessToken throws`() = runTest {
        coEvery { tmdbAuthRepository.revokeAccessToken(any()) } throws Exception("network error")
        val useCase = RevokeTmdbAuthUseCase(tmdbAuthRepository, tokenRepository)

        useCase("tok")

        coVerify(exactly = 1) { tokenRepository.clearTokens() }
    }

    @Test
    fun `RevokeTmdbAuthUseCase clears local auth even when revokeAccessToken throws RuntimeException`() = runTest {
        coEvery { tmdbAuthRepository.revokeAccessToken(any()) } throws RuntimeException("server error")
        val useCase = RevokeTmdbAuthUseCase(tmdbAuthRepository, tokenRepository)

        useCase("tok")

        coVerify(exactly = 1) { tokenRepository.clearTokens() }
    }

    // --- SubmitTmdbRatingUseCase ---

    @Test
    fun `SubmitTmdbRatingUseCase returns false when session ID is null`() = runTest {
        coEvery { tokenRepository.getSessionIdOnce() } returns null
        val useCase = SubmitTmdbRatingUseCase(tmdbAuthRepository, tokenRepository)

        val result = useCase(movieId = 1, rating = 4.0f)

        assertFalse(result)
        coVerify(exactly = 0) { tmdbAuthRepository.rateMovie(any(), any(), any()) }
    }

    @Test
    fun `SubmitTmdbRatingUseCase delegates to repository and returns true on success`() = runTest {
        coEvery { tokenRepository.getSessionIdOnce() } returns "sess_abc"
        coEvery { tmdbAuthRepository.rateMovie(movieId = 99, sessionId = "sess_abc", rating = 3.5f) } returns true
        val useCase = SubmitTmdbRatingUseCase(tmdbAuthRepository, tokenRepository)

        val result = useCase(movieId = 99, rating = 3.5f)

        assertTrue(result)
        coVerify(exactly = 1) { tmdbAuthRepository.rateMovie(99, "sess_abc", 3.5f) }
    }

    @Test
    fun `SubmitTmdbRatingUseCase returns false when repository rateMovie returns false`() = runTest {
        coEvery { tokenRepository.getSessionIdOnce() } returns "sess_abc"
        coEvery { tmdbAuthRepository.rateMovie(any(), any(), any()) } returns false
        val useCase = SubmitTmdbRatingUseCase(tmdbAuthRepository, tokenRepository)

        val result = useCase(movieId = 5, rating = 2.0f)

        assertFalse(result)
    }

    @Test
    fun `SubmitTmdbRatingUseCase passes correct movieId and rating to rateMovie`() = runTest {
        coEvery { tokenRepository.getSessionIdOnce() } returns "sess_xyz"
        coEvery { tmdbAuthRepository.rateMovie(movieId = 7, sessionId = "sess_xyz", rating = 5.0f) } returns true
        val useCase = SubmitTmdbRatingUseCase(tmdbAuthRepository, tokenRepository)

        useCase(movieId = 7, rating = 5.0f)

        coVerify(exactly = 1) { tmdbAuthRepository.rateMovie(movieId = 7, sessionId = "sess_xyz", rating = 5.0f) }
    }
}
