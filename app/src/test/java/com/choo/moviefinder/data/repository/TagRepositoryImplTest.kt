package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.MovieTagDao
import com.choo.moviefinder.data.local.entity.FavoriteMovieEntity
import com.choo.moviefinder.data.local.entity.MovieTagEntity
import com.choo.moviefinder.domain.model.MovieTag
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TagRepositoryImplTest {

    private lateinit var movieTagDao: MovieTagDao
    private lateinit var repository: TagRepositoryImpl

    @Before
    fun setUp() {
        movieTagDao = mockk()
        repository = TagRepositoryImpl(movieTagDao)
    }

    // --- getTagsForMovie ---

    @Test
    fun `getTagsForMovie maps MovieTagEntity list to MovieTag domain list`() = runTest {
        val entity = MovieTagEntity(id = 1L, movieId = 10, tagName = "action", addedAt = 1000L)
        every { movieTagDao.getTagsByMovieId(10) } returns flowOf(listOf(entity))

        val result = repository.getTagsForMovie(10).first()

        assertEquals(1, result.size)
        val tag = result.first()
        assertEquals(1L, tag.id)
        assertEquals(10, tag.movieId)
        assertEquals("action", tag.tagName)
        assertEquals(1000L, tag.addedAt)
    }

    @Test
    fun `getTagsForMovie returns empty list when dao returns empty`() = runTest {
        every { movieTagDao.getTagsByMovieId(99) } returns flowOf(emptyList())

        val result = repository.getTagsForMovie(99).first()

        assertEquals(emptyList<MovieTag>(), result)
    }

    @Test
    fun `getTagsForMovie maps multiple entities correctly`() = runTest {
        val entities = listOf(
            MovieTagEntity(id = 1L, movieId = 5, tagName = "drama", addedAt = 100L),
            MovieTagEntity(id = 2L, movieId = 5, tagName = "comedy", addedAt = 200L)
        )
        every { movieTagDao.getTagsByMovieId(5) } returns flowOf(entities)

        val result = repository.getTagsForMovie(5).first()

        assertEquals(2, result.size)
        assertEquals("drama", result[0].tagName)
        assertEquals("comedy", result[1].tagName)
    }

    // --- getAllTagNames ---

    @Test
    fun `getAllTagNames delegates to dao`() = runTest {
        every { movieTagDao.getAllDistinctTagNames() } returns flowOf(listOf("action", "drama"))

        val result = repository.getAllTagNames().first()

        verify(exactly = 1) { movieTagDao.getAllDistinctTagNames() }
        assertEquals(listOf("action", "drama"), result)
    }

    @Test
    fun `getAllTagNames returns empty list when dao returns empty`() = runTest {
        every { movieTagDao.getAllDistinctTagNames() } returns flowOf(emptyList())

        val result = repository.getAllTagNames().first()

        assertEquals(emptyList<String>(), result)
    }

    // --- addTag ---

    @Test
    fun `addTag calls dao insertTag with correct entity`() = runTest {
        val slot = slot<MovieTagEntity>()
        coEvery { movieTagDao.insertTag(capture(slot)) } returns Unit

        repository.addTag(10, "action")

        coVerify(exactly = 1) { movieTagDao.insertTag(any()) }
        assertEquals(10, slot.captured.movieId)
        assertEquals("action", slot.captured.tagName)
    }

    @Test
    fun `addTag trims whitespace from tagName before inserting`() = runTest {
        val slot = slot<MovieTagEntity>()
        coEvery { movieTagDao.insertTag(capture(slot)) } returns Unit

        repository.addTag(10, "  sci-fi  ")

        assertEquals("sci-fi", slot.captured.tagName)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `addTag throws IllegalArgumentException when tagName is blank`() = runTest {
        repository.addTag(10, "   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `addTag throws IllegalArgumentException when tagName is empty string`() = runTest {
        repository.addTag(10, "")
    }

    // --- removeTag ---

    @Test
    fun `removeTag calls dao deleteTag with correct arguments`() = runTest {
        coEvery { movieTagDao.deleteTag(10, "action") } returns Unit

        repository.removeTag(10, "action")

        coVerify(exactly = 1) { movieTagDao.deleteTag(10, "action") }
    }

    @Test
    fun `removeTag passes movieId and tagName correctly`() = runTest {
        val capturedIds = mutableListOf<Int>()
        val capturedNames = mutableListOf<String>()
        coEvery {
            movieTagDao.deleteTag(capture(capturedIds), capture(capturedNames))
        } returns Unit

        repository.removeTag(42, "thriller")

        assertEquals(42, capturedIds.first())
        assertEquals("thriller", capturedNames.first())
    }

    // --- getFavoritesByTag ---

    @Test
    fun `getFavoritesByTag delegates to dao and maps entities`() = runTest {
        val entity = FavoriteMovieEntity(
            id = 1,
            title = "Fav Movie",
            posterPath = "/p.jpg",
            backdropPath = null,
            overview = "Overview",
            releaseDate = "2024-01-01",
            voteAverage = 8.0,
            voteCount = 200
        )
        every { movieTagDao.getFavoritesByTag("action") } returns flowOf(listOf(entity))

        val result = repository.getFavoritesByTag("action").first()

        verify(exactly = 1) { movieTagDao.getFavoritesByTag("action") }
        assertEquals(1, result.size)
        assertEquals(1, result.first().id)
        assertEquals("Fav Movie", result.first().title)
    }

    @Test
    fun `getFavoritesByTag returns empty list when no favorites match`() = runTest {
        every { movieTagDao.getFavoritesByTag("unknown") } returns flowOf(emptyList())

        val result = repository.getFavoritesByTag("unknown").first()

        assertEquals(emptyList<com.choo.moviefinder.domain.model.Movie>(), result)
    }
}
