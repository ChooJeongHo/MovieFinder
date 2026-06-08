package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.MemoDao
import com.choo.moviefinder.data.local.entity.MemoEntity
import com.choo.moviefinder.domain.model.MemoConstants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MemoRepositoryImplTest {

    private lateinit var memoDao: MemoDao
    private lateinit var repository: MemoRepositoryImpl

    @Before
    fun setUp() {
        memoDao = mockk(relaxUnitFun = true)
        repository = MemoRepositoryImpl(memoDao)
    }

    // --- getMemos ---

    @Test
    fun `getMemos returns mapped memos from dao`() = runTest {
        val entity = MemoEntity(id = 1L, movieId = 10, content = "note")
        every { memoDao.getMemosByMovieId(10) } returns flowOf(listOf(entity))

        val result = repository.getMemos(10).first()

        assertEquals(1, result.size)
        assertEquals("note", result[0].content)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getMemos throws for non-positive movieId`() = runTest {
        repository.getMemos(0).first()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getMemos throws for negative movieId`() = runTest {
        repository.getMemos(-1).first()
    }

    // --- saveMemo ---

    @Test
    fun `saveMemo inserts entity with correct movieId and content`() = runTest {
        val slot = slot<MemoEntity>()
        coEvery { memoDao.insert(capture(slot)) } returns Unit

        repository.saveMemo(5, "My note")

        coVerify(exactly = 1) { memoDao.insert(any()) }
        assertEquals(5, slot.captured.movieId)
        assertEquals("My note", slot.captured.content)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `saveMemo throws for non-positive movieId`() = runTest {
        repository.saveMemo(0, "content")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `saveMemo throws for blank content`() = runTest {
        repository.saveMemo(1, "   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `saveMemo throws when content exceeds MAX_LENGTH`() = runTest {
        repository.saveMemo(1, "x".repeat(MemoConstants.MAX_LENGTH + 1))
    }

    @Test
    fun `saveMemo accepts content exactly at MAX_LENGTH`() = runTest {
        repository.saveMemo(1, "x".repeat(MemoConstants.MAX_LENGTH))

        coVerify(exactly = 1) { memoDao.insert(any()) }
    }

    // --- updateMemo ---

    @Test
    fun `updateMemo calls dao with correct memoId and content`() = runTest {
        repository.updateMemo(memoId = 1L, content = "updated note")

        coVerify(exactly = 1) { memoDao.updateMemo(memoId = 1L, content = "updated note", updatedAt = any()) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateMemo throws for blank content`() = runTest {
        repository.updateMemo(memoId = 1L, content = "  ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateMemo throws when content exceeds MAX_LENGTH`() = runTest {
        repository.updateMemo(memoId = 1L, content = "x".repeat(MemoConstants.MAX_LENGTH + 1))
    }

    @Test
    fun `updateMemo accepts content exactly at MAX_LENGTH`() = runTest {
        repository.updateMemo(memoId = 1L, content = "x".repeat(MemoConstants.MAX_LENGTH))

        coVerify(exactly = 1) { memoDao.updateMemo(any(), any(), any()) }
    }

    // --- deleteMemo ---

    @Test
    fun `deleteMemo delegates to dao`() = runTest {
        repository.deleteMemo(memoId = 7L)

        coVerify(exactly = 1) { memoDao.deleteMemo(7L) }
    }
}
