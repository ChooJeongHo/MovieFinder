package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.DomainException
import com.choo.moviefinder.domain.repository.LocalizedTitleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetLocalizedTitleUseCaseTest {

    private lateinit var repository: LocalizedTitleRepository
    private lateinit var useCase: GetLocalizedTitleUseCase

    @Before
    fun setUp() {
        repository = mockk()
        every { repository.needsLocalization() } returns true
        every { repository.peekTitle(any()) } returns null
        useCase = GetLocalizedTitleUseCase(repository)
    }

    // --- peek ---

    @Test
    fun `peek returns fallback without touching cache when localization is not needed`() {
        every { repository.needsLocalization() } returns false

        val result = useCase.peek(1, "배트맨")

        assertEquals("배트맨", result)
        verify(exactly = 0) { repository.peekTitle(any()) }
    }

    @Test
    fun `peek returns cached localized title when available`() {
        every { repository.peekTitle(1) } returns "The Batman"

        assertEquals("The Batman", useCase.peek(1, "배트맨"))
    }

    @Test
    fun `peek returns fallback on cache miss`() {
        assertEquals("배트맨", useCase.peek(1, "배트맨"))
    }

    // --- invoke ---

    @Test
    fun `invoke returns fallback immediately without network when localization is not needed`() = runTest {
        every { repository.needsLocalization() } returns false

        val result = useCase(1, "배트맨")

        assertEquals("배트맨", result)
        assertEquals(0L, currentTime)
        coVerify(exactly = 0) { repository.getTitle(any()) }
    }

    @Test
    fun `invoke returns cached title without debounce delay or network`() = runTest {
        every { repository.peekTitle(1) } returns "The Batman"

        val result = useCase(1, "배트맨")

        assertEquals("The Batman", result)
        assertEquals(0L, currentTime)
        coVerify(exactly = 0) { repository.getTitle(any()) }
    }

    @Test
    fun `invoke queries repository only after debounce elapses`() = runTest {
        coEvery { repository.getTitle(1) } returns "The Batman"

        val deferred = async { useCase(1, "배트맨") }
        advanceTimeBy(149)
        runCurrent()
        coVerify(exactly = 0) { repository.getTitle(any()) }

        advanceTimeBy(1)
        runCurrent()
        assertEquals("The Batman", deferred.await())
        coVerify(exactly = 1) { repository.getTitle(1) }
    }

    @Test
    fun `invoke cancelled during debounce never queries repository`() = runTest {
        coEvery { repository.getTitle(any()) } returns "The Batman"

        val deferred = async { useCase(1, "배트맨") }
        advanceTimeBy(100)
        deferred.cancel()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.getTitle(any()) }
    }

    @Test
    fun `invoke returns fallback when repository throws DomainException`() = runTest {
        coEvery { repository.getTitle(1) } throws DomainException.NetworkError(RuntimeException())

        assertEquals("배트맨", useCase(1, "배트맨"))
    }

    @Test
    fun `invoke returns fallback when repository returns blank title`() = runTest {
        coEvery { repository.getTitle(1) } returns "  "

        assertEquals("배트맨", useCase(1, "배트맨"))
    }

    @Test(expected = CancellationException::class)
    fun `invoke rethrows CancellationException from repository`() = runTest {
        coEvery { repository.getTitle(1) } throws CancellationException("cancelled")

        useCase(1, "배트맨")
    }
}
