package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.WatchHistoryRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetRecentShortcutMoviesUseCaseTest {

    private lateinit var repository: WatchHistoryRepository
    private lateinit var useCase: GetRecentShortcutMoviesUseCase

    private fun movie(id: Int, title: String = "Movie $id") = Movie(
        id = id,
        title = title,
        posterPath = "/poster$id.jpg",
        backdropPath = null,
        overview = "Overview $id",
        releaseDate = "2024-01-01",
        voteAverage = 7.0,
        voteCount = 100
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetRecentShortcutMoviesUseCase(repository)
    }

    @Test
    fun `invoke deduplicates movies with the same id`() = runTest {
        val duplicated = listOf(movie(1), movie(1), movie(2))
        every { repository.getWatchHistory() } returns flowOf(duplicated)

        val result = useCase()

        assertEquals(listOf(movie(1), movie(2)), result)
    }

    @Test
    fun `invoke returns at most limit movies`() = runTest {
        val many = listOf(movie(1), movie(2), movie(3), movie(4))
        every { repository.getWatchHistory() } returns flowOf(many)

        val result = useCase(limit = 2)

        assertEquals(2, result.size)
        assertEquals(listOf(movie(1), movie(2)), result)
    }

    @Test
    fun `invoke returns empty list when watch history is empty`() = runTest {
        every { repository.getWatchHistory() } returns flowOf(emptyList())

        val result = useCase()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke uses default limit of MAX_SHORTCUT_MOVIES`() = runTest {
        val many = listOf(movie(1), movie(2), movie(3))
        every { repository.getWatchHistory() } returns flowOf(many)

        val result = useCase()

        assertEquals(GetRecentShortcutMoviesUseCase.MAX_SHORTCUT_MOVIES, result.size)
    }
}
