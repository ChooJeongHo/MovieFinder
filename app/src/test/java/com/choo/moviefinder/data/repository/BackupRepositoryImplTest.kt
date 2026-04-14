package com.choo.moviefinder.data.repository

import androidx.room.withTransaction
import com.choo.moviefinder.data.local.MovieDatabase
import com.choo.moviefinder.data.local.dao.FavoriteMovieDao
import com.choo.moviefinder.data.local.dao.MemoDao
import com.choo.moviefinder.data.local.dao.MovieTagDao
import com.choo.moviefinder.data.local.dao.UserRatingDao
import com.choo.moviefinder.data.local.dao.WatchlistDao
import com.choo.moviefinder.data.local.entity.FavoriteMovieEntity
import com.choo.moviefinder.data.local.entity.MemoEntity
import com.choo.moviefinder.data.local.entity.MovieTagEntity
import com.choo.moviefinder.data.local.entity.UserRatingEntity
import com.choo.moviefinder.domain.model.BackupMemo
import com.choo.moviefinder.domain.model.BackupMovie
import com.choo.moviefinder.domain.model.BackupRating
import com.choo.moviefinder.domain.model.BackupTag
import com.choo.moviefinder.domain.model.UserDataBackup
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupRepositoryImplTest {

    private lateinit var database: MovieDatabase
    private lateinit var favoriteMovieDao: FavoriteMovieDao
    private lateinit var watchlistDao: WatchlistDao
    private lateinit var userRatingDao: UserRatingDao
    private lateinit var memoDao: MemoDao
    private lateinit var movieTagDao: MovieTagDao
    private lateinit var repository: BackupRepositoryImpl

    private val testFavoriteEntity = FavoriteMovieEntity(
        id = 1, title = "Test Movie", posterPath = "/poster.jpg",
        backdropPath = "/backdrop.jpg", overview = "Overview",
        releaseDate = "2024-01-01", voteAverage = 8.0, voteCount = 100, addedAt = 1000L
    )

    @Before
    fun setup() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        database = mockk()
        favoriteMovieDao = mockk(relaxed = true)
        watchlistDao = mockk(relaxed = true)
        userRatingDao = mockk(relaxed = true)
        memoDao = mockk(relaxed = true)
        movieTagDao = mockk(relaxed = true)
        repository = BackupRepositoryImpl(
            database, favoriteMovieDao, watchlistDao, userRatingDao, memoDao, movieTagDao
        )
        coEvery { database.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            @Suppress("UNCHECKED_CAST")
            (secondArg<suspend () -> Any?>()).invoke()
        }
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    // ── exportUserData ──────────────────────────────────────────

    @Test
    fun `exportUserData maps favorites correctly`() = runTest {
        coEvery { favoriteMovieDao.getAllFavoritesOnce() } returns listOf(testFavoriteEntity)
        coEvery { watchlistDao.getAllWatchlistOnce() } returns emptyList()
        coEvery { userRatingDao.getAllRatings() } returns emptyList()
        coEvery { memoDao.getAllMemos() } returns emptyList()
        coEvery { movieTagDao.getAllTagsOnce() } returns emptyList()

        val backup = repository.exportUserData()

        assertEquals(1, backup.favorites.size)
        assertEquals(1, backup.favorites[0].id)
        assertEquals("Test Movie", backup.favorites[0].title)
        assertEquals(1000L, backup.favorites[0].addedAt)
    }

    @Test
    fun `exportUserData maps ratings memos and tags correctly`() = runTest {
        coEvery { favoriteMovieDao.getAllFavoritesOnce() } returns emptyList()
        coEvery { watchlistDao.getAllWatchlistOnce() } returns emptyList()
        coEvery { userRatingDao.getAllRatings() } returns listOf(UserRatingEntity(movieId = 1, rating = 4.5f))
        coEvery { memoDao.getAllMemos() } returns listOf(
            MemoEntity(movieId = 1, content = "Great movie", createdAt = 2000L, updatedAt = 3000L)
        )
        coEvery { movieTagDao.getAllTagsOnce() } returns listOf(
            MovieTagEntity(movieId = 1, tagName = "Action", addedAt = 4000L)
        )

        val backup = repository.exportUserData()

        assertEquals(1, backup.ratings.size)
        assertEquals(4.5f, backup.ratings[0].rating)
        assertEquals("Great movie", backup.memos[0].content)
        assertEquals(2000L, backup.memos[0].createdAt)
        assertEquals("Action", backup.tags[0].tagName)
        assertEquals(4000L, backup.tags[0].addedAt)
    }

    @Test
    fun `exportUserData with all empty DAOs returns empty backup`() = runTest {
        coEvery { favoriteMovieDao.getAllFavoritesOnce() } returns emptyList()
        coEvery { watchlistDao.getAllWatchlistOnce() } returns emptyList()
        coEvery { userRatingDao.getAllRatings() } returns emptyList()
        coEvery { memoDao.getAllMemos() } returns emptyList()
        coEvery { movieTagDao.getAllTagsOnce() } returns emptyList()

        val backup = repository.exportUserData()

        assertTrue(backup.favorites.isEmpty())
        assertTrue(backup.watchlist.isEmpty())
        assertTrue(backup.ratings.isEmpty())
        assertTrue(backup.memos.isEmpty())
        assertTrue(backup.tags.isEmpty())
    }

    // ── importUserData ──────────────────────────────────────────

    @Test
    fun `importUserData calls insertAll for non-empty favorites and watchlist`() = runTest {
        val backup = UserDataBackup(
            favorites = listOf(BackupMovie(1, "Fav", null, 0.0, "", addedAt = 1000L)),
            watchlist = listOf(BackupMovie(2, "Watch", null, 0.0, "", addedAt = 2000L)),
            ratings = emptyList(), memos = emptyList(), tags = emptyList()
        )

        repository.importUserData(backup)

        coVerify { favoriteMovieDao.insertAll(any()) }
        coVerify { watchlistDao.insertAll(any()) }
    }

    @Test
    fun `importUserData calls insertAll for ratings memos and tags`() = runTest {
        val backup = UserDataBackup(
            favorites = emptyList(), watchlist = emptyList(),
            ratings = listOf(BackupRating(movieId = 1, rating = 4.0f)),
            memos = listOf(BackupMemo(movieId = 1, content = "memo")),
            tags = listOf(BackupTag(movieId = 1, tagName = "Drama"))
        )

        repository.importUserData(backup)

        coVerify { userRatingDao.insertAll(any()) }
        coVerify { memoDao.insertAll(any()) }
        coVerify { movieTagDao.insertAll(any()) }
    }

    @Test
    fun `importUserData preserves non-zero addedAt timestamp`() = runTest {
        val backup = UserDataBackup(
            favorites = listOf(BackupMovie(1, "Fav", null, 0.0, "", addedAt = 9999L)),
            watchlist = emptyList(), ratings = emptyList(), memos = emptyList(), tags = emptyList()
        )

        repository.importUserData(backup)

        coVerify { favoriteMovieDao.insertAll(match { it.first().addedAt == 9999L }) }
    }

    @Test
    fun `importUserData uses current time when addedAt is zero`() = runTest {
        val before = System.currentTimeMillis()
        val backup = UserDataBackup(
            favorites = listOf(BackupMovie(1, "Fav", null, 0.0, "", addedAt = 0L)),
            watchlist = emptyList(), ratings = emptyList(), memos = emptyList(), tags = emptyList()
        )

        repository.importUserData(backup)
        val after = System.currentTimeMillis()

        coVerify { favoriteMovieDao.insertAll(match { it.first().addedAt in before..after }) }
    }

    @Test
    fun `importUserData skips insertAll when all lists are empty`() = runTest {
        repository.importUserData(
            UserDataBackup(
                favorites = emptyList(), watchlist = emptyList(), ratings = emptyList(),
                memos = emptyList(), tags = emptyList()
            )
        )

        coVerify(exactly = 0) { favoriteMovieDao.insertAll(any()) }
        coVerify(exactly = 0) { watchlistDao.insertAll(any()) }
        coVerify(exactly = 0) { userRatingDao.insertAll(any()) }
        coVerify(exactly = 0) { memoDao.insertAll(any()) }
        coVerify(exactly = 0) { movieTagDao.insertAll(any()) }
    }
}
