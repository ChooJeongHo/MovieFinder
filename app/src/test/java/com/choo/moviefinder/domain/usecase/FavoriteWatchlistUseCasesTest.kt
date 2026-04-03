package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.FavoriteRepository
import com.choo.moviefinder.domain.repository.WatchlistRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FavoriteWatchlistUseCasesTest {

    private lateinit var favoriteRepository: FavoriteRepository
    private lateinit var watchlistRepository: WatchlistRepository

    private val testMovie = Movie(
        id = 1,
        title = "Test Movie",
        posterPath = "/poster.jpg",
        backdropPath = "/backdrop.jpg",
        overview = "Overview",
        releaseDate = "2024-01-01",
        voteAverage = 7.5,
        voteCount = 1000
    )

    @Before
    fun setUp() {
        favoriteRepository = mockk()
        watchlistRepository = mockk()
    }

    // --- GetFavoriteMoviesUseCase ---

    @Test
    fun `GetFavoriteMoviesUseCase invokes getFavoriteMovies on repository`() {
        val flow = flowOf(listOf(testMovie))
        every { favoriteRepository.getFavoriteMovies() } returns flow
        val useCase = GetFavoriteMoviesUseCase(favoriteRepository)

        val result = useCase()

        verify(exactly = 1) { favoriteRepository.getFavoriteMovies() }
        assertEquals(flow, result)
    }

    @Test
    fun `GetFavoriteMoviesUseCase returns list from repository`() = runTest {
        every { favoriteRepository.getFavoriteMovies() } returns flowOf(listOf(testMovie))
        val useCase = GetFavoriteMoviesUseCase(favoriteRepository)

        val result = useCase().first()

        assertEquals(listOf(testMovie), result)
    }

    // --- IsFavoriteUseCase ---

    @Test
    fun `IsFavoriteUseCase invokes isFavorite with correct movieId`() {
        every { favoriteRepository.isFavorite(42) } returns flowOf(true)
        val useCase = IsFavoriteUseCase(favoriteRepository)

        useCase(42)

        verify(exactly = 1) { favoriteRepository.isFavorite(42) }
    }

    @Test
    fun `IsFavoriteUseCase returns correct boolean from repository`() = runTest {
        every { favoriteRepository.isFavorite(1) } returns flowOf(false)
        val useCase = IsFavoriteUseCase(favoriteRepository)

        val result = useCase(1).first()

        assertEquals(false, result)
    }

    // --- ToggleFavoriteUseCase ---

    @Test
    fun `ToggleFavoriteUseCase invokes toggleFavorite on repository`() = runTest {
        coEvery { favoriteRepository.toggleFavorite(testMovie) } returns Unit
        val useCase = ToggleFavoriteUseCase(favoriteRepository)

        useCase(testMovie)

        coVerify(exactly = 1) { favoriteRepository.toggleFavorite(testMovie) }
    }

    @Test
    fun `ToggleFavoriteUseCase passes correct movie to repository`() = runTest {
        val capturedMovies = mutableListOf<Movie>()
        coEvery { favoriteRepository.toggleFavorite(capture(capturedMovies)) } returns Unit
        val useCase = ToggleFavoriteUseCase(favoriteRepository)

        useCase(testMovie)

        assertEquals(testMovie, capturedMovies.first())
    }

    // --- GetWatchlistUseCase ---

    @Test
    fun `GetWatchlistUseCase invokes getWatchlistMovies on repository`() {
        val flow = flowOf(listOf(testMovie))
        every { watchlistRepository.getWatchlistMovies() } returns flow
        val useCase = GetWatchlistUseCase(watchlistRepository)

        val result = useCase()

        verify(exactly = 1) { watchlistRepository.getWatchlistMovies() }
        assertEquals(flow, result)
    }

    @Test
    fun `GetWatchlistUseCase returns list from repository`() = runTest {
        every { watchlistRepository.getWatchlistMovies() } returns flowOf(listOf(testMovie))
        val useCase = GetWatchlistUseCase(watchlistRepository)

        val result = useCase().first()

        assertEquals(listOf(testMovie), result)
    }

    // --- IsInWatchlistUseCase ---

    @Test
    fun `IsInWatchlistUseCase invokes isInWatchlist with correct movieId`() {
        every { watchlistRepository.isInWatchlist(99) } returns flowOf(true)
        val useCase = IsInWatchlistUseCase(watchlistRepository)

        useCase(99)

        verify(exactly = 1) { watchlistRepository.isInWatchlist(99) }
    }

    @Test
    fun `IsInWatchlistUseCase returns correct boolean from repository`() = runTest {
        every { watchlistRepository.isInWatchlist(5) } returns flowOf(true)
        val useCase = IsInWatchlistUseCase(watchlistRepository)

        val result = useCase(5).first()

        assertEquals(true, result)
    }

    // --- ToggleWatchlistUseCase ---

    @Test
    fun `ToggleWatchlistUseCase invokes toggleWatchlist on repository`() = runTest {
        coEvery { watchlistRepository.toggleWatchlist(testMovie) } returns Unit
        val useCase = ToggleWatchlistUseCase(watchlistRepository)

        useCase(testMovie)

        coVerify(exactly = 1) { watchlistRepository.toggleWatchlist(testMovie) }
    }

    @Test
    fun `ToggleWatchlistUseCase passes correct movie to repository`() = runTest {
        val capturedMovies = mutableListOf<Movie>()
        coEvery { watchlistRepository.toggleWatchlist(capture(capturedMovies)) } returns Unit
        val useCase = ToggleWatchlistUseCase(watchlistRepository)

        useCase(testMovie)

        assertEquals(testMovie, capturedMovies.first())
    }
}
