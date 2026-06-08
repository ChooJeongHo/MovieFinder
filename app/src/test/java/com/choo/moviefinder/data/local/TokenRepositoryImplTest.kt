package com.choo.moviefinder.data.local

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TokenRepositoryImplTest {

    private lateinit var secureTokenStore: SecureTokenStore
    private lateinit var repository: TokenRepositoryImpl

    @Before
    fun setUp() {
        secureTokenStore = mockk(relaxUnitFun = true)
        every { secureTokenStore.getAccessToken() } returns null
        repository = TokenRepositoryImpl(secureTokenStore)
    }

    @Test
    fun `getAccessToken emits initial value from SecureTokenStore`() = runTest {
        every { secureTokenStore.getAccessToken() } returns "initial_token"
        val repo = TokenRepositoryImpl(secureTokenStore)

        val result = repo.getAccessToken().first()

        assertEquals("initial_token", result)
    }

    @Test
    fun `getAccessToken emits null when no token stored`() = runTest {
        val result = repository.getAccessToken().first()

        assertNull(result)
    }

    @Test
    fun `saveTokens persists to SecureTokenStore and updates flow`() = runTest {
        repository.saveTokens("tok", "acct", "sess")

        verify(exactly = 1) { secureTokenStore.saveTokens("tok", "acct", "sess") }
        assertEquals("tok", repository.getAccessToken().first())
    }

    @Test
    fun `clearTokens clears store and sets flow to null`() = runTest {
        repository.saveTokens("tok", "acct", "sess")

        repository.clearTokens()

        verify(exactly = 1) { secureTokenStore.clearTokens() }
        assertNull(repository.getAccessToken().first())
    }

    @Test
    fun `getSessionIdOnce delegates to SecureTokenStore`() = runTest {
        every { secureTokenStore.getSessionId() } returns "sess_123"

        val result = repository.getSessionIdOnce()

        assertEquals("sess_123", result)
        verify(exactly = 1) { secureTokenStore.getSessionId() }
    }

    @Test
    fun `getAuthOnce returns pair of accessToken and accountId`() = runTest {
        every { secureTokenStore.getAccessToken() } returns "tok"
        every { secureTokenStore.getAccountId() } returns "acct"

        val result = repository.getAuthOnce()

        assertEquals(Pair("tok", "acct"), result)
    }

    @Test
    fun `getAuthOnce returns null pair when no tokens stored`() = runTest {
        every { secureTokenStore.getAccessToken() } returns null
        every { secureTokenStore.getAccountId() } returns null

        val result = repository.getAuthOnce()

        assertEquals(Pair(null, null), result)
    }
}
