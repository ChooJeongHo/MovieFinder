package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.PreferencesRepository
import com.choo.moviefinder.domain.repository.TmdbAuthRepository
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
import java.io.IOException

class TmdbAuthUseCasesTest {

    private lateinit var tmdbAuthRepository: TmdbAuthRepository
    private lateinit var preferencesRepository: PreferencesRepository

    @Before
    fun setUp() {
        tmdbAuthRepository = mockk()
        preferencesRepository = mockk(relaxUnitFun = true)
    }

    // --- GetTmdbAccessTokenUseCase ---

    @Test
    fun `GetTmdbAccessTokenUseCase delegates to PreferencesRepository`() {
        val flow = flowOf("access_token_123")
        every { preferencesRepository.getTmdbAccessToken() } returns flow
        val useCase = GetTmdbAccessTokenUseCase(preferencesRepository)

        val result = useCase()

        verify(exactly = 1) { preferencesRepository.getTmdbAccessToken() }
        assertEquals(flow, result)
    }

    @Test
    fun `GetTmdbAccessTokenUseCase returns null-emitting flow when no token stored`() {
        every { preferencesRepository.getTmdbAccessToken() } returns flowOf(null)
        val useCase = GetTmdbAccessTokenUseCase(preferencesRepository)

        val result = useCase()

        assertEquals(flowOf<String?>(null).javaClass, result.javaClass)
        verify(exactly = 1) { preferencesRepository.getTmdbAccessToken() }
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
    fun `ExchangeTmdbTokenUseCase exchanges token and saves auth to PreferencesRepository`() = runTest {
        val triple = Triple("access_token", "account_id", "session_id")
        coEvery { tmdbAuthRepository.exchangeAccessToken("req_token") } returns triple
        val useCase = ExchangeTmdbTokenUseCase(tmdbAuthRepository, preferencesRepository)

        useCase("req_token")

        coVerify(exactly = 1) { tmdbAuthRepository.exchangeAccessToken("req_token") }
        coVerify(exactly = 1) {
            preferencesRepository.saveTmdbAuth(
                accessToken = "access_token",
                accountId = "account_id",
                sessionId = "session_id"
            )
        }
    }

    @Test
    fun `ExchangeTmdbTokenUseCase passes exact Triple values from repository to saveTmdbAuth`() = runTest {
        val triple = Triple("tok123", "acct456", "sess789")
        coEvery { tmdbAuthRepository.exchangeAccessToken(any()) } returns triple
        val useCase = ExchangeTmdbTokenUseCase(tmdbAuthRepository, preferencesRepository)

        useCase("any_token")

        coVerify(exactly = 1) {
            preferencesRepository.saveTmdbAuth("tok123", "acct456", "sess789")
        }
    }

    // --- RevokeTmdbAuthUseCase ---

    @Test
    fun `RevokeTmdbAuthUseCase revokes token and clears local auth`() = runTest {
        coEvery { tmdbAuthRepository.revokeAccessToken("tok") } returns Unit
        val useCase = RevokeTmdbAuthUseCase(tmdbAuthRepository, preferencesRepository)

        useCase("tok")

        coVerify(exactly = 1) { tmdbAuthRepository.revokeAccessToken("tok") }
        coVerify(exactly = 1) { preferencesRepository.clearTmdbAuth() }
    }

    @Test
    fun `RevokeTmdbAuthUseCase clears local auth even when revokeAccessToken throws`() = runTest {
        coEvery { tmdbAuthRepository.revokeAccessToken(any()) } throws IOException("network error")
        val useCase = RevokeTmdbAuthUseCase(tmdbAuthRepository, preferencesRepository)

        useCase("tok")

        // Local auth must still be cleared despite the API failure
        coVerify(exactly = 1) { preferencesRepository.clearTmdbAuth() }
    }

    @Test
    fun `RevokeTmdbAuthUseCase clears local auth even when revokeAccessToken throws RuntimeException`() = runTest {
        coEvery { tmdbAuthRepository.revokeAccessToken(any()) } throws RuntimeException("server error")
        val useCase = RevokeTmdbAuthUseCase(tmdbAuthRepository, preferencesRepository)

        useCase("tok")

        coVerify(exactly = 1) { preferencesRepository.clearTmdbAuth() }
    }

    // --- SubmitTmdbRatingUseCase ---

    @Test
    fun `SubmitTmdbRatingUseCase returns false when session ID is null`() = runTest {
        coEvery { preferencesRepository.getTmdbSessionIdOnce() } returns null
        val useCase = SubmitTmdbRatingUseCase(tmdbAuthRepository, preferencesRepository)

        val result = useCase(movieId = 1, rating = 4.0f)

        assertFalse(result)
        coVerify(exactly = 0) { tmdbAuthRepository.rateMovie(any(), any(), any()) }
    }

    @Test
    fun `SubmitTmdbRatingUseCase delegates to repository and returns true on success`() = runTest {
        coEvery { preferencesRepository.getTmdbSessionIdOnce() } returns "sess_abc"
        coEvery { tmdbAuthRepository.rateMovie(movieId = 99, sessionId = "sess_abc", rating = 3.5f) } returns true
        val useCase = SubmitTmdbRatingUseCase(tmdbAuthRepository, preferencesRepository)

        val result = useCase(movieId = 99, rating = 3.5f)

        assertTrue(result)
        coVerify(exactly = 1) { tmdbAuthRepository.rateMovie(99, "sess_abc", 3.5f) }
    }

    @Test
    fun `SubmitTmdbRatingUseCase returns false when repository rateMovie returns false`() = runTest {
        coEvery { preferencesRepository.getTmdbSessionIdOnce() } returns "sess_abc"
        coEvery { tmdbAuthRepository.rateMovie(any(), any(), any()) } returns false
        val useCase = SubmitTmdbRatingUseCase(tmdbAuthRepository, preferencesRepository)

        val result = useCase(movieId = 5, rating = 2.0f)

        assertFalse(result)
    }

    @Test
    fun `SubmitTmdbRatingUseCase passes correct movieId and rating to rateMovie`() = runTest {
        coEvery { preferencesRepository.getTmdbSessionIdOnce() } returns "sess_xyz"
        coEvery { tmdbAuthRepository.rateMovie(movieId = 7, sessionId = "sess_xyz", rating = 5.0f) } returns true
        val useCase = SubmitTmdbRatingUseCase(tmdbAuthRepository, preferencesRepository)

        useCase(movieId = 7, rating = 5.0f)

        coVerify(exactly = 1) { tmdbAuthRepository.rateMovie(movieId = 7, sessionId = "sess_xyz", rating = 5.0f) }
    }
}
