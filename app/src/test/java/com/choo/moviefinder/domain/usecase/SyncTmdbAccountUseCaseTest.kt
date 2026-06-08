package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.FavoriteRepository
import com.choo.moviefinder.domain.repository.TmdbAuthRepository
import com.choo.moviefinder.domain.repository.TokenRepository
import com.choo.moviefinder.domain.repository.WatchlistRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SyncTmdbAccountUseCaseTest {

    private lateinit var tmdbAuthRepository: TmdbAuthRepository
    private lateinit var tokenRepository: TokenRepository
    private lateinit var favoriteRepository: FavoriteRepository
    private lateinit var watchlistRepository: WatchlistRepository
    private lateinit var useCase: SyncTmdbAccountUseCase

    private fun movie(id: Int) = Movie(
        id = id, title = "Movie $id", posterPath = null,
        overview = "", releaseDate = "2024-01-01",
        voteAverage = 7.0, backdropPath = null, voteCount = 100
    )

    @Before
    fun setUp() {
        tmdbAuthRepository = mockk()
        tokenRepository = mockk()
        favoriteRepository = mockk(relaxUnitFun = true)
        watchlistRepository = mockk(relaxUnitFun = true)
        useCase = SyncTmdbAccountUseCase(tmdbAuthRepository, tokenRepository, favoriteRepository, watchlistRepository)
    }

    @Test
    fun `sync adds new favorites and watchlist entries from TMDB`() = runTest {
        val movie1 = movie(1)
        val movie2 = movie(2)
        coEvery { tokenRepository.getAuthOnce() } returns Pair("token", "acct")
        coEvery { tmdbAuthRepository.getAccountFavorites("acct", "Bearer token") } returns listOf(movie1)
        coEvery { tmdbAuthRepository.getAccountWatchlist("acct", "Bearer token") } returns listOf(movie2)
        every { favoriteRepository.isFavorite(1) } returns flowOf(false)
        every { watchlistRepository.isInWatchlist(2) } returns flowOf(false)

        val result = useCase()

        assertEquals(1, result.favoritesAdded)
        assertEquals(1, result.watchlistAdded)
        coVerify(exactly = 1) { favoriteRepository.toggleFavorite(movie1) }
        coVerify(exactly = 1) { watchlistRepository.toggleWatchlist(movie2) }
    }

    @Test
    fun `sync skips movies already in favorites`() = runTest {
        val movie1 = movie(1)
        coEvery { tokenRepository.getAuthOnce() } returns Pair("token", "acct")
        coEvery { tmdbAuthRepository.getAccountFavorites("acct", "Bearer token") } returns listOf(movie1)
        coEvery { tmdbAuthRepository.getAccountWatchlist("acct", "Bearer token") } returns emptyList()
        every { favoriteRepository.isFavorite(1) } returns flowOf(true)

        val result = useCase()

        assertEquals(0, result.favoritesAdded)
        coVerify(exactly = 0) { favoriteRepository.toggleFavorite(any()) }
    }

    @Test
    fun `sync skips movies already in watchlist`() = runTest {
        val movie2 = movie(2)
        coEvery { tokenRepository.getAuthOnce() } returns Pair("token", "acct")
        coEvery { tmdbAuthRepository.getAccountFavorites("acct", "Bearer token") } returns emptyList()
        coEvery { tmdbAuthRepository.getAccountWatchlist("acct", "Bearer token") } returns listOf(movie2)
        every { watchlistRepository.isInWatchlist(2) } returns flowOf(true)

        val result = useCase()

        assertEquals(0, result.watchlistAdded)
        coVerify(exactly = 0) { watchlistRepository.toggleWatchlist(any()) }
    }

    @Test
    fun `sync returns zero counts when TMDB lists are empty`() = runTest {
        coEvery { tokenRepository.getAuthOnce() } returns Pair("token", "acct")
        coEvery { tmdbAuthRepository.getAccountFavorites(any(), any()) } returns emptyList()
        coEvery { tmdbAuthRepository.getAccountWatchlist(any(), any()) } returns emptyList()

        val result = useCase()

        assertEquals(0, result.favoritesAdded)
        assertEquals(0, result.watchlistAdded)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `sync throws when accessToken is null`() = runTest {
        coEvery { tokenRepository.getAuthOnce() } returns Pair(null, "acct")

        useCase()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `sync throws when accountId is null`() = runTest {
        coEvery { tokenRepository.getAuthOnce() } returns Pair("token", null)

        useCase()
    }

    @Test
    fun `sync constructs bearer token correctly`() = runTest {
        coEvery { tokenRepository.getAuthOnce() } returns Pair("mytoken123", "acct42")
        coEvery { tmdbAuthRepository.getAccountFavorites("acct42", "Bearer mytoken123") } returns emptyList()
        coEvery { tmdbAuthRepository.getAccountWatchlist("acct42", "Bearer mytoken123") } returns emptyList()

        useCase()

        coVerify(exactly = 1) { tmdbAuthRepository.getAccountFavorites("acct42", "Bearer mytoken123") }
        coVerify(exactly = 1) { tmdbAuthRepository.getAccountWatchlist("acct42", "Bearer mytoken123") }
    }

    @Test
    fun `sync counts only newly added movies not skipped ones`() = runTest {
        val newMovie = movie(1)
        val existingMovie = movie(2)
        coEvery { tokenRepository.getAuthOnce() } returns Pair("token", "acct")
        coEvery { tmdbAuthRepository.getAccountFavorites(any(), any()) } returns listOf(newMovie, existingMovie)
        coEvery { tmdbAuthRepository.getAccountWatchlist(any(), any()) } returns emptyList()
        every { favoriteRepository.isFavorite(1) } returns flowOf(false)
        every { favoriteRepository.isFavorite(2) } returns flowOf(true)

        val result = useCase()

        assertEquals(1, result.favoritesAdded)
        coVerify(exactly = 1) { favoriteRepository.toggleFavorite(newMovie) }
        coVerify(exactly = 0) { favoriteRepository.toggleFavorite(existingMovie) }
    }
}
