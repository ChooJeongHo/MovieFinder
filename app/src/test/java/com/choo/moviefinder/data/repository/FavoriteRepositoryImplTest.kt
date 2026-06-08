package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.FavoriteMovieDao
import com.choo.moviefinder.data.local.entity.FavoriteMovieEntity
import com.choo.moviefinder.domain.model.FavoriteSortOrder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FavoriteRepositoryImplTest {

    private lateinit var favoriteMovieDao: FavoriteMovieDao
    private lateinit var repository: FavoriteRepositoryImpl

    private fun entity(id: Int, title: String = "Movie $id") = FavoriteMovieEntity(
        id = id, title = title, posterPath = null, backdropPath = null,
        overview = "", releaseDate = "2024-01-01", voteAverage = 7.0, voteCount = 0
    )

    @Before
    fun setUp() {
        favoriteMovieDao = mockk()
        repository = FavoriteRepositoryImpl(favoriteMovieDao)
    }

    @Test
    fun `getFavoriteMovies maps entities to domain models`() = runTest {
        every { favoriteMovieDao.getAllFavorites() } returns flowOf(listOf(entity(1), entity(2)))

        val result = repository.getFavoriteMovies().first()

        assertEquals(2, result.size)
        assertEquals(1, result[0].id)
        assertEquals(2, result[1].id)
    }

    @Test
    fun `getFavoriteMovies returns empty list when dao returns empty`() = runTest {
        every { favoriteMovieDao.getAllFavorites() } returns flowOf(emptyList())

        val result = repository.getFavoriteMovies().first()

        assertEquals(emptyList<com.choo.moviefinder.domain.model.Movie>(), result)
    }

    @Test
    fun `getFavoriteMoviesSorted ADDED_DATE delegates to getAllFavorites`() = runTest {
        every { favoriteMovieDao.getAllFavorites() } returns flowOf(listOf(entity(1)))

        val result = repository.getFavoriteMoviesSorted(FavoriteSortOrder.ADDED_DATE).first()

        assertEquals(1, result.size)
    }

    @Test
    fun `getFavoriteMoviesSorted TITLE delegates to getAllFavoritesSortedByTitle`() = runTest {
        every { favoriteMovieDao.getAllFavoritesSortedByTitle() } returns flowOf(listOf(entity(1)))

        val result = repository.getFavoriteMoviesSorted(FavoriteSortOrder.TITLE).first()

        assertEquals(1, result.size)
    }

    @Test
    fun `getFavoriteMoviesSorted RATING delegates to getAllFavoritesSortedByRating`() = runTest {
        every { favoriteMovieDao.getAllFavoritesSortedByRating() } returns flowOf(listOf(entity(1)))

        val result = repository.getFavoriteMoviesSorted(FavoriteSortOrder.RATING).first()

        assertEquals(1, result.size)
    }

    @Test
    fun `isFavorite delegates to dao`() = runTest {
        every { favoriteMovieDao.isFavorite(10) } returns flowOf(true)

        val result = repository.isFavorite(10).first()

        assertEquals(true, result)
    }

    @Test
    fun `searchFavoriteMovies maps entities to domain models`() = runTest {
        coEvery { favoriteMovieDao.searchFavorites("dune") } returns listOf(entity(5, "Dune"))

        val result = repository.searchFavoriteMovies("dune")

        assertEquals(1, result.size)
        assertEquals(5, result[0].id)
        coVerify(exactly = 1) { favoriteMovieDao.searchFavorites("dune") }
    }
}
