package com.choo.moviefinder.data.repository

import com.choo.moviefinder.core.util.AppLanguageProvider
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.MovieDetailDto
import com.choo.moviefinder.domain.model.DomainException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class LocalizedTitleRepositoryImplTest {

    private lateinit var apiService: MovieApiService
    private lateinit var appLanguageProvider: AppLanguageProvider
    private lateinit var repository: LocalizedTitleRepositoryImpl

    @Before
    fun setUp() {
        apiService = mockk()
        appLanguageProvider = mockk { every { currentApiLanguage() } returns "en-US" }
        repository = LocalizedTitleRepositoryImpl(apiService, appLanguageProvider)
    }

    private fun detail(id: Int, title: String) = MovieDetailDto(id = id, title = title)

    // --- needsLocalization ---

    @Test
    fun `needsLocalization is true when app language is English`() {
        assertTrue(repository.needsLocalization())
    }

    @Test
    fun `needsLocalization is false when app language is Korean`() {
        every { appLanguageProvider.currentApiLanguage() } returns "ko-KR"

        assertFalse(repository.needsLocalization())
    }

    // --- getTitle ---

    @Test
    fun `getTitle requests detail in current app language and returns its title`() = runTest {
        coEvery { apiService.getMovieDetail(1, "en-US") } returns detail(1, "The Batman")

        assertEquals("The Batman", repository.getTitle(1))
        coVerify(exactly = 1) { apiService.getMovieDetail(1, "en-US") }
    }

    @Test
    fun `getTitle serves repeated lookups from cache without hitting the api again`() = runTest {
        coEvery { apiService.getMovieDetail(1, "en-US") } returns detail(1, "The Batman")

        repository.getTitle(1)
        val second = repository.getTitle(1)

        assertEquals("The Batman", second)
        coVerify(exactly = 1) { apiService.getMovieDetail(any(), any()) }
    }

    @Test
    fun `getTitle does not cache blank titles so they are retried later`() = runTest {
        coEvery { apiService.getMovieDetail(1, "en-US") } returns detail(1, "")

        repository.getTitle(1)
        repository.getTitle(1)

        coVerify(exactly = 2) { apiService.getMovieDetail(1, "en-US") }
        assertNull(repository.peekTitle(1))
    }

    @Test
    fun `getTitle wraps network failures as DomainException and leaves nothing cached`() = runTest {
        coEvery { apiService.getMovieDetail(1, "en-US") } throws IOException("offline")

        val failure = runCatching { repository.getTitle(1) }.exceptionOrNull()

        assertTrue(failure is DomainException.NetworkError)
        assertNull(repository.peekTitle(1))
    }

    @Test
    fun `getTitle rejects non-positive movie id`() = runTest {
        val failure = runCatching { repository.getTitle(0) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    // --- peekTitle ---

    @Test
    fun `peekTitle returns null before lookup and cached title after`() = runTest {
        coEvery { apiService.getMovieDetail(1, "en-US") } returns detail(1, "The Batman")
        assertNull(repository.peekTitle(1))

        repository.getTitle(1)

        assertEquals("The Batman", repository.peekTitle(1))
    }

    @Test
    fun `cache is keyed by language so a title from another language is never returned`() = runTest {
        coEvery { apiService.getMovieDetail(1, "en-US") } returns detail(1, "The Batman")
        repository.getTitle(1)

        every { appLanguageProvider.currentApiLanguage() } returns "ko-KR"
        assertNull(repository.peekTitle(1))

        every { appLanguageProvider.currentApiLanguage() } returns "en-US"
        assertEquals("The Batman", repository.peekTitle(1))
    }

    @Test
    fun `cache evicts least recently used entries instead of growing without bound`() = runTest {
        coEvery { apiService.getMovieDetail(any(), "en-US") } answers {
            detail(firstArg(), "Title ${firstArg<Int>()}")
        }

        // 캐시 상한(500)보다 충분히 많이 채워 가장 오래된 항목이 밀려나는지 확인
        (1..MORE_THAN_CAPACITY).forEach { repository.getTitle(it) }

        assertNull(repository.peekTitle(1))
        assertNotNull(repository.peekTitle(MORE_THAN_CAPACITY))
    }

    // --- 동시 요청 제한 ---

    @Test
    fun `concurrent lookups never exceed four in-flight api calls`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var inFlight = 0
        var maxInFlight = 0
        coEvery { apiService.getMovieDetail(any(), "en-US") } coAnswers {
            inFlight++
            maxInFlight = maxOf(maxInFlight, inFlight)
            gate.await()
            inFlight--
            detail(firstArg(), "Title ${firstArg<Int>()}")
        }

        val lookups = (1..10).map { id -> async { repository.getTitle(id) } }
        runCurrent()
        // 게이트가 닫힌 동안 시작된 호출은 세마포어 허용치(4)까지만이어야 한다
        assertEquals(4, inFlight)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals((1..10).map { "Title $it" }, lookups.awaitAll())
        assertEquals(4, maxInFlight)
    }

    private companion object {
        const val MORE_THAN_CAPACITY = 600
    }
}
