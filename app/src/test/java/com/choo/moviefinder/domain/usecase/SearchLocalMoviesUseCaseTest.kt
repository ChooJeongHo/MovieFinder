package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.FavoriteRepository
import com.choo.moviefinder.domain.repository.WatchlistRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SearchLocalMoviesUseCaseTest {

    private lateinit var favoriteRepository: FavoriteRepository
    private lateinit var watchlistRepository: WatchlistRepository
    private lateinit var useCase: SearchLocalMoviesUseCase

    private fun movie(id: Int, title: String = "Movie $id") = Movie(
        id = id, title = title, posterPath = null, backdropPath = null,
        overview = "", releaseDate = "2024-01-01", voteAverage = 7.0, voteCount = 0
    )

    @Before
    fun setUp() {
        favoriteRepository = mockk()
        watchlistRepository = mockk()
        useCase = SearchLocalMoviesUseCase(favoriteRepository, watchlistRepository)
    }

    @Test
    fun `invoke combines favorites and watchlist results`() = runTest {
        coEvery { favoriteRepository.searchFavoriteMovies("dune") } returns listOf(movie(1))
        coEvery { watchlistRepository.searchWatchlistMovies("dune") } returns listOf(movie(2))

        val result = useCase("dune")

        assertEquals(2, result.size)
        coVerify(exactly = 1) { favoriteRepository.searchFavoriteMovies("dune") }
        coVerify(exactly = 1) { watchlistRepository.searchWatchlistMovies("dune") }
    }

    @Test
    fun `invoke deduplicates movies that appear in both favorites and watchlist`() = runTest {
        val sharedMovie = movie(1, "Dune")
        coEvery { favoriteRepository.searchFavoriteMovies("dune") } returns listOf(sharedMovie)
        coEvery { watchlistRepository.searchWatchlistMovies("dune") } returns listOf(sharedMovie)

        val result = useCase("dune")

        assertEquals(1, result.size)
        assertEquals(1, result[0].id)
    }

    @Test
    fun `invoke returns empty list when both repositories return empty`() = runTest {
        coEvery { favoriteRepository.searchFavoriteMovies(any()) } returns emptyList()
        coEvery { watchlistRepository.searchWatchlistMovies(any()) } returns emptyList()

        val result = useCase("xyz")

        assertEquals(0, result.size)
    }

    @Test
    fun `invoke returns only favorites when watchlist is empty`() = runTest {
        coEvery { favoriteRepository.searchFavoriteMovies("action") } returns listOf(movie(1), movie(2))
        coEvery { watchlistRepository.searchWatchlistMovies("action") } returns emptyList()

        val result = useCase("action")

        assertEquals(2, result.size)
    }

    @Test
    fun `invoke deduplicates by id keeping first occurrence from favorites`() = runTest {
        val favVersion = movie(1, "Fav Title")
        val watchVersion = movie(1, "Watch Title")
        coEvery { favoriteRepository.searchFavoriteMovies(any()) } returns listOf(favVersion)
        coEvery { watchlistRepository.searchWatchlistMovies(any()) } returns listOf(watchVersion)

        val result = useCase("movie")

        assertEquals(1, result.size)
        assertEquals("Fav Title", result[0].title)
    }
}
