package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.RecentSearchDao
import com.choo.moviefinder.data.local.entity.RecentSearchEntity
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

class SearchHistoryRepositoryImplTest {

    private lateinit var recentSearchDao: RecentSearchDao
    private lateinit var repository: SearchHistoryRepositoryImpl

    @Before
    fun setUp() {
        recentSearchDao = mockk(relaxUnitFun = true)
        repository = SearchHistoryRepositoryImpl(recentSearchDao)
    }

    // --- getRecentSearches ---

    @Test
    fun `getRecentSearches maps entity query strings to list`() = runTest {
        val entities = listOf(
            RecentSearchEntity(query = "avengers"),
            RecentSearchEntity(query = "batman")
        )
        every { recentSearchDao.getRecentSearches() } returns flowOf(entities)

        val result = repository.getRecentSearches().first()

        assertEquals(listOf("avengers", "batman"), result)
    }

    @Test
    fun `getRecentSearches returns empty list when dao returns empty`() = runTest {
        every { recentSearchDao.getRecentSearches() } returns flowOf(emptyList())

        val result = repository.getRecentSearches().first()

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `getRecentSearches returns single-element list`() = runTest {
        every { recentSearchDao.getRecentSearches() } returns
            flowOf(listOf(RecentSearchEntity(query = "inception")))

        val result = repository.getRecentSearches().first()

        assertEquals(listOf("inception"), result)
    }

    // --- saveSearchQuery ---

    @Test
    fun `saveSearchQuery inserts entity with trimmed query`() = runTest {
        val slot = slot<RecentSearchEntity>()
        coEvery { recentSearchDao.insert(capture(slot)) } returns Unit

        repository.saveSearchQuery("  iron man  ")

        coVerify(exactly = 1) { recentSearchDao.insert(any()) }
        assertEquals("iron man", slot.captured.query)
    }

    @Test
    fun `saveSearchQuery collapses internal whitespace to single space`() = runTest {
        val slot = slot<RecentSearchEntity>()
        coEvery { recentSearchDao.insert(capture(slot)) } returns Unit

        repository.saveSearchQuery("spider   man")

        assertEquals("spider man", slot.captured.query)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `saveSearchQuery throws for blank query`() = runTest {
        repository.saveSearchQuery("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `saveSearchQuery throws for empty string`() = runTest {
        repository.saveSearchQuery("")
    }

    @Test
    fun `saveSearchQuery inserts exactly once for valid query`() = runTest {
        repository.saveSearchQuery("thor")

        coVerify(exactly = 1) { recentSearchDao.insert(any()) }
    }

    // --- deleteSearchQuery ---

    @Test
    fun `deleteSearchQuery passes trimmed query to dao`() = runTest {
        repository.deleteSearchQuery("  batman  ")

        coVerify(exactly = 1) { recentSearchDao.delete("batman") }
    }

    @Test
    fun `deleteSearchQuery collapses internal whitespace before deleting`() = runTest {
        repository.deleteSearchQuery("dark   knight")

        coVerify(exactly = 1) { recentSearchDao.delete("dark knight") }
    }

    @Test
    fun `deleteSearchQuery passes correct already-trimmed query`() = runTest {
        repository.deleteSearchQuery("wonder woman")

        coVerify(exactly = 1) { recentSearchDao.delete("wonder woman") }
    }

    // --- clearSearchHistory ---

    @Test
    fun `clearSearchHistory calls dao clearAll`() = runTest {
        repository.clearSearchHistory()

        coVerify(exactly = 1) { recentSearchDao.clearAll() }
    }

    @Test
    fun `clearSearchHistory calls dao exactly once`() = runTest {
        repository.clearSearchHistory()
        repository.clearSearchHistory()

        coVerify(exactly = 2) { recentSearchDao.clearAll() }
    }
}
