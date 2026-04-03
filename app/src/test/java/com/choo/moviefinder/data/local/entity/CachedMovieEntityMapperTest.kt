package com.choo.moviefinder.data.local.entity

import com.choo.moviefinder.domain.model.Movie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CachedMovieEntityMapperTest {

    private val testEntity = CachedMovieEntity(
        id = 1,
        category = "now_playing",
        title = "Test Movie",
        posterPath = "/poster.jpg",
        backdropPath = "/backdrop.jpg",
        overview = "Overview text",
        releaseDate = "2024-01-01",
        voteAverage = 7.5,
        voteCount = 1000,
        page = 1,
        cachedAt = 123456789L
    )

    private val testMovie = Movie(
        id = 1,
        title = "Test Movie",
        posterPath = "/poster.jpg",
        backdropPath = "/backdrop.jpg",
        overview = "Overview text",
        releaseDate = "2024-01-01",
        voteAverage = 7.5,
        voteCount = 1000
    )

    // --- toDomain ---

    @Test
    fun `toDomain maps all fields correctly`() {
        val result = testEntity.toDomain()

        assertEquals(testEntity.id, result.id)
        assertEquals(testEntity.title, result.title)
        assertEquals(testEntity.posterPath, result.posterPath)
        assertEquals(testEntity.backdropPath, result.backdropPath)
        assertEquals(testEntity.overview, result.overview)
        assertEquals(testEntity.releaseDate, result.releaseDate)
        assertEquals(testEntity.voteAverage, result.voteAverage, 0.0)
        assertEquals(testEntity.voteCount, result.voteCount)
    }

    @Test
    fun `toDomain handles null posterPath correctly`() {
        val entityWithNullPoster = testEntity.copy(posterPath = null)

        val result = entityWithNullPoster.toDomain()

        assertNull(result.posterPath)
    }

    @Test
    fun `toDomain handles null backdropPath correctly`() {
        val entityWithNullBackdrop = testEntity.copy(backdropPath = null)

        val result = entityWithNullBackdrop.toDomain()

        assertNull(result.backdropPath)
    }

    // --- toCachedEntity ---

    @Test
    fun `toCachedEntity maps all fields correctly including category and page`() {
        val result = testMovie.toCachedEntity(category = "popular", page = 3)

        assertEquals(testMovie.id, result.id)
        assertEquals("popular", result.category)
        assertEquals(testMovie.title, result.title)
        assertEquals(testMovie.posterPath, result.posterPath)
        assertEquals(testMovie.backdropPath, result.backdropPath)
        assertEquals(testMovie.overview, result.overview)
        assertEquals(testMovie.releaseDate, result.releaseDate)
        assertEquals(testMovie.voteAverage, result.voteAverage, 0.0)
        assertEquals(testMovie.voteCount, result.voteCount)
        assertEquals(3, result.page)
    }

    @Test
    fun `toCachedEntity preserves null posterPath`() {
        val movieWithNullPoster = testMovie.copy(posterPath = null)

        val result = movieWithNullPoster.toCachedEntity(category = "now_playing", page = 1)

        assertNull(result.posterPath)
    }

    // --- round-trip ---

    @Test
    fun `toCachedEntity then toDomain returns equivalent Movie`() {
        val cached = testMovie.toCachedEntity(category = "now_playing", page = 1)
        val restored = cached.toDomain()

        assertEquals(testMovie.id, restored.id)
        assertEquals(testMovie.title, restored.title)
        assertEquals(testMovie.posterPath, restored.posterPath)
        assertEquals(testMovie.backdropPath, restored.backdropPath)
        assertEquals(testMovie.overview, restored.overview)
        assertEquals(testMovie.releaseDate, restored.releaseDate)
        assertEquals(testMovie.voteAverage, restored.voteAverage, 0.0)
        assertEquals(testMovie.voteCount, restored.voteCount)
    }

    @Test
    fun `round-trip preserves null poster and backdrop`() {
        val movieWithNulls = testMovie.copy(posterPath = null, backdropPath = null)

        val restored = movieWithNulls.toCachedEntity("trending", 2).toDomain()

        assertNull(restored.posterPath)
        assertNull(restored.backdropPath)
    }
}
