package com.choo.moviefinder.data.repository

import app.cash.turbine.test
import com.choo.moviefinder.data.local.MovieDatabase
import com.choo.moviefinder.data.local.dao.FavoriteMovieDao
import com.choo.moviefinder.data.local.dao.MemoDao
import com.choo.moviefinder.data.local.dao.RecentSearchDao
import com.choo.moviefinder.data.local.dao.UserRatingDao
import com.choo.moviefinder.data.local.dao.WatchHistoryDao
import com.choo.moviefinder.data.local.dao.MovieTagDao
import com.choo.moviefinder.data.local.dao.WatchlistDao
import com.choo.moviefinder.data.local.entity.FavoriteMovieEntity
import com.choo.moviefinder.data.local.entity.MemoEntity
import com.choo.moviefinder.data.local.entity.RecentSearchEntity
import com.choo.moviefinder.data.local.entity.UserRatingEntity
import com.choo.moviefinder.data.local.entity.WatchHistoryEntity
import com.choo.moviefinder.data.local.entity.WatchlistEntity
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.MovieDto
import com.choo.moviefinder.data.remote.dto.PersonCreditsResponse
import com.choo.moviefinder.data.remote.dto.PersonDetailDto
import com.choo.moviefinder.data.util.Constants
import com.choo.moviefinder.domain.model.BackupMemo
import com.choo.moviefinder.domain.model.BackupMovie
import com.choo.moviefinder.domain.model.BackupRating
import com.choo.moviefinder.domain.model.MemoConstants
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.UserDataBackup
import androidx.room.withTransaction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplitRepositoryImplTest {

    private lateinit var favoriteMovieDao: FavoriteMovieDao
    private lateinit var recentSearchDao: RecentSearchDao
    private lateinit var watchHistoryDao: WatchHistoryDao
    private lateinit var watchlistDao: WatchlistDao
    private lateinit var userRatingDao: UserRatingDao
    private lateinit var memoDao: MemoDao
    private lateinit var movieTagDao: MovieTagDao
    private lateinit var apiService: MovieApiService
    private lateinit var database: MovieDatabase

    private lateinit var favoriteRepo: FavoriteRepositoryImpl
    private lateinit var watchlistRepo: WatchlistRepositoryImpl
    private lateinit var searchHistoryRepo: SearchHistoryRepositoryImpl
    private lateinit var watchHistoryRepo: WatchHistoryRepositoryImpl
    private lateinit var userRatingRepo: UserRatingRepositoryImpl
    private lateinit var memoRepo: MemoRepositoryImpl
    private lateinit var personRepo: PersonRepositoryImpl
    private lateinit var backupRepo: BackupRepositoryImpl

    private val testFavoriteEntities = listOf(
        FavoriteMovieEntity(
            id = 1, title = "Fav 1", posterPath = "/f1.jpg",
            backdropPath = "/fb1.jpg", overview = "Fav overview 1",
            releaseDate = "2024-01-01", voteAverage = 8.0, voteCount = 100
        ),
        FavoriteMovieEntity(
            id = 2, title = "Fav 2", posterPath = "/f2.jpg",
            backdropPath = "/fb2.jpg", overview = "Fav overview 2",
            releaseDate = "2024-02-01", voteAverage = 7.5, voteCount = 200
        )
    )

    private val testMovieDtos = listOf(
        MovieDto(
            id = 10, title = "Similar 1", posterPath = "/s1.jpg",
            overview = "Overview 1", releaseDate = "2024-02-01",
            voteAverage = 7.0, voteCount = 500
        ),
        MovieDto(
            id = 11, title = "Similar 2", posterPath = "/s2.jpg",
            overview = "Overview 2", releaseDate = "2024-03-01",
            voteAverage = 6.5, voteCount = 300
        )
    )

    @Before
    fun setup() {
        mockkStatic("androidx.room.RoomDatabaseKt__RoomDatabase_androidKt")
        favoriteMovieDao = mockk(relaxUnitFun = true)
        recentSearchDao = mockk(relaxUnitFun = true)
        watchHistoryDao = mockk(relaxUnitFun = true)
        watchlistDao = mockk(relaxUnitFun = true)
        userRatingDao = mockk(relaxUnitFun = true)
        memoDao = mockk(relaxUnitFun = true)
        movieTagDao = mockk(relaxUnitFun = true)
        apiService = mockk()
        database = mockk()
        coEvery { database.withTransaction<Unit>(any()) } coAnswers {
            @Suppress("UNCHECKED_CAST")
            (invocation.args[1] as suspend () -> Unit).invoke()
        }

        favoriteRepo = FavoriteRepositoryImpl(favoriteMovieDao)
        watchlistRepo = WatchlistRepositoryImpl(watchlistDao)
        searchHistoryRepo = SearchHistoryRepositoryImpl(recentSearchDao)
        watchHistoryRepo = WatchHistoryRepositoryImpl(watchHistoryDao)
        userRatingRepo = UserRatingRepositoryImpl(userRatingDao)
        memoRepo = MemoRepositoryImpl(memoDao)
        personRepo = PersonRepositoryImpl(apiService)
        backupRepo = BackupRepositoryImpl(database, favoriteMovieDao, watchlistDao, userRatingDao, memoDao, movieTagDao)
    }

    // --- FavoriteRepository ---

    @Test
    fun `toggleFavorite delegates to dao toggleFavorite`() = runTest {
        val movie = Movie(1, "Test", "/p.jpg", "/b.jpg", "Overview", "2024-01-01", 8.0, 100)

        favoriteRepo.toggleFavorite(movie)

        coVerify { favoriteMovieDao.toggleFavorite(match { it.id == 1 && it.title == "Test" }) }
    }

    @Test
    fun `isFavorite returns flow from dao`() = runTest {
        every { favoriteMovieDao.isFavorite(1) } returns flowOf(true)

        favoriteRepo.isFavorite(1).test {
            assertTrue(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `isFavorite returns false for non-favorite`() = runTest {
        every { favoriteMovieDao.isFavorite(99) } returns flowOf(false)

        favoriteRepo.isFavorite(99).test {
            assertFalse(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getFavoriteMovies returns mapped domain movies`() = runTest {
        every { favoriteMovieDao.getAllFavorites() } returns flowOf(testFavoriteEntities)

        favoriteRepo.getFavoriteMovies().test {
            val movies = awaitItem()
            assertEquals(2, movies.size)
            assertEquals("Fav 1", movies[0].title)
            assertEquals(1, movies[0].id)
            assertEquals("Fav 2", movies[1].title)
            assertEquals(2, movies[1].id)
            awaitComplete()
        }
    }

    @Test
    fun `getFavoriteMovies returns empty list when no favorites`() = runTest {
        every { favoriteMovieDao.getAllFavorites() } returns flowOf(emptyList())

        favoriteRepo.getFavoriteMovies().test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    // --- SearchHistoryRepository ---

    @Test
    fun `getRecentSearches returns query strings`() = runTest {
        val entities = listOf(
            RecentSearchEntity(query = "Batman"),
            RecentSearchEntity(query = "Spider-Man")
        )
        every { recentSearchDao.getRecentSearches() } returns flowOf(entities)

        searchHistoryRepo.getRecentSearches().test {
            val searches = awaitItem()
            assertEquals(2, searches.size)
            assertEquals("Batman", searches[0])
            assertEquals("Spider-Man", searches[1])
            awaitComplete()
        }
    }

    @Test
    fun `saveSearchQuery inserts entity into dao`() = runTest {
        searchHistoryRepo.saveSearchQuery("Avengers")

        coVerify { recentSearchDao.insert(match { it.query == "Avengers" }) }
    }

    @Test
    fun `deleteSearchQuery calls dao delete`() = runTest {
        searchHistoryRepo.deleteSearchQuery("Batman")

        coVerify { recentSearchDao.delete("Batman") }
    }

    @Test
    fun `clearSearchHistory calls dao clearAll`() = runTest {
        searchHistoryRepo.clearSearchHistory()

        coVerify { recentSearchDao.clearAll() }
    }

    // --- WatchHistoryRepository ---

    @Test
    fun `getWatchHistory returns mapped domain movies`() = runTest {
        val entities = listOf(
            WatchHistoryEntity(
                id = 1, title = "Movie 1", posterPath = "/p1.jpg",
                backdropPath = "/b1.jpg", overview = "Overview 1",
                releaseDate = "2024-01-01", voteAverage = 8.0, voteCount = 500
            ),
            WatchHistoryEntity(
                id = 2, title = "Movie 2", posterPath = "/p2.jpg",
                backdropPath = "/b2.jpg", overview = "Overview 2",
                releaseDate = "2024-02-01", voteAverage = 7.5, voteCount = 300
            )
        )
        every { watchHistoryDao.getRecentHistory(Constants.WATCH_HISTORY_LIMIT) } returns flowOf(entities)

        watchHistoryRepo.getWatchHistory().test {
            val movies = awaitItem()
            assertEquals(2, movies.size)
            assertEquals("Movie 1", movies[0].title)
            assertEquals("Movie 2", movies[1].title)
            awaitComplete()
        }
    }

    @Test
    fun `saveWatchHistory delegates to dao insert`() = runTest {
        val movie = Movie(1, "Test", "/p.jpg", "/b.jpg", "Overview", "2024-01-01", 8.0, 100)

        watchHistoryRepo.saveWatchHistory(movie)

        coVerify { watchHistoryDao.insert(match { it.id == 1 && it.title == "Test" }) }
    }

    @Test
    fun `clearWatchHistory delegates to dao clearAll`() = runTest {
        watchHistoryRepo.clearWatchHistory()

        coVerify { watchHistoryDao.clearAll() }
    }

    // --- WatchlistRepository ---

    @Test
    fun `getWatchlistMovies returns mapped domain movies`() = runTest {
        val entities = listOf(
            WatchlistEntity(
                id = 1, title = "Watch 1", posterPath = "/p1.jpg",
                backdropPath = "/b1.jpg", overview = "O1",
                releaseDate = "2024-01-01", voteAverage = 8.0, voteCount = 100
            ),
            WatchlistEntity(
                id = 2, title = "Watch 2", posterPath = "/p2.jpg",
                backdropPath = "/b2.jpg", overview = "O2",
                releaseDate = "2024-02-01", voteAverage = 7.5, voteCount = 200
            )
        )
        every { watchlistDao.getAllWatchlist() } returns flowOf(entities)

        watchlistRepo.getWatchlistMovies().test {
            val movies = awaitItem()
            assertEquals(2, movies.size)
            assertEquals("Watch 1", movies[0].title)
            assertEquals("Watch 2", movies[1].title)
            awaitComplete()
        }
    }

    @Test
    fun `toggleWatchlist delegates to watchlistDao`() = runTest {
        val movie = Movie(1, "Test", "/p.jpg", "/b.jpg", "Overview", "2024-01-01", 8.0, 100)

        watchlistRepo.toggleWatchlist(movie)

        coVerify { watchlistDao.toggleWatchlist(match { it.id == 1 && it.title == "Test" }) }
    }

    @Test
    fun `isInWatchlist returns true from dao`() = runTest {
        every { watchlistDao.isInWatchlist(1) } returns flowOf(true)

        watchlistRepo.isInWatchlist(1).test {
            assertTrue(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `isInWatchlist returns false for non-watchlist movie`() = runTest {
        every { watchlistDao.isInWatchlist(99) } returns flowOf(false)

        watchlistRepo.isInWatchlist(99).test {
            assertFalse(awaitItem())
            awaitComplete()
        }
    }

    // --- UserRatingRepository ---

    @Test
    fun `getUserRating returns flow from dao`() = runTest {
        every { userRatingDao.getRating(1) } returns flowOf(4.5f)

        userRatingRepo.getUserRating(1).test {
            assertEquals(4.5f, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `setUserRating delegates to dao insertRating`() = runTest {
        userRatingRepo.setUserRating(1, 4.0f)

        coVerify {
            userRatingDao.insertRating(match { it.movieId == 1 && it.rating == 4.0f })
        }
    }

    @Test
    fun `deleteUserRating delegates to dao deleteRating`() = runTest {
        userRatingRepo.deleteUserRating(1)

        coVerify { userRatingDao.deleteRating(1) }
    }

    // --- PersonRepository ---

    @Test
    fun `getPersonDetail returns mapped domain model`() = runTest {
        val dto = PersonDetailDto(
            id = 42,
            name = "Test Actor",
            biography = "Some bio",
            birthday = "1980-01-01",
            placeOfBirth = "Los Angeles",
            profilePath = "/profile.jpg",
            knownForDepartment = "Acting"
        )
        coEvery { apiService.getPersonDetail(42) } returns dto

        val result = personRepo.getPersonDetail(42)

        assertEquals(42, result.id)
        assertEquals("Test Actor", result.name)
        assertEquals("Some bio", result.biography)
        assertEquals("1980-01-01", result.birthday)
        assertEquals("Acting", result.knownForDepartment)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getPersonDetail throws on invalid personId`() = runTest {
        personRepo.getPersonDetail(0)
    }

    @Test
    fun `getPersonMovieCredits returns mapped movies`() = runTest {
        coEvery { apiService.getPersonMovieCredits(5) } returns PersonCreditsResponse(
            cast = testMovieDtos
        )

        val result = personRepo.getPersonMovieCredits(5)

        assertEquals(2, result.size)
        assertEquals("Similar 1", result[0].title)
        assertEquals("Similar 2", result[1].title)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getPersonMovieCredits throws on invalid personId`() = runTest {
        personRepo.getPersonMovieCredits(-1)
    }

    // --- BackupRepository ---

    @Test
    fun `exportUserData returns backup with all data`() = runTest {
        val favEntities = testFavoriteEntities
        val watchlistEntities = listOf(
            WatchlistEntity(
                id = 10, title = "Watch 1", posterPath = "/w1.jpg",
                backdropPath = null, overview = "O1",
                releaseDate = "2024-01-01", voteAverage = 7.0, voteCount = 100
            )
        )
        val ratingEntities = listOf(UserRatingEntity(movieId = 1, rating = 4.5f))
        val memoEntities = listOf(MemoEntity(movieId = 1, content = "Great film"))

        coEvery { favoriteMovieDao.getAllFavoritesOnce() } returns favEntities
        coEvery { watchlistDao.getAllWatchlistOnce() } returns watchlistEntities
        coEvery { userRatingDao.getAllRatings() } returns ratingEntities
        coEvery { memoDao.getAllMemos() } returns memoEntities
        coEvery { movieTagDao.getAllTagsOnce() } returns emptyList()

        val result = backupRepo.exportUserData()

        assertEquals(2, result.favorites.size)
        assertEquals(1, result.watchlist.size)
        assertEquals(1, result.ratings.size)
        assertEquals(1, result.memos.size)
        assertEquals("Fav 1", result.favorites[0].title)
        assertEquals("Watch 1", result.watchlist[0].title)
        assertEquals(4.5f, result.ratings[0].rating)
        assertEquals("Great film", result.memos[0].content)
    }

    // --- MemoRepository ---

    @Test
    fun `getMemos returns mapped domain memos`() = runTest {
        val entities = listOf(
            MemoEntity(movieId = 1, content = "첫 번째 메모"),
            MemoEntity(movieId = 1, content = "두 번째 메모")
        )
        every { memoDao.getMemosByMovieId(1) } returns flowOf(entities)

        memoRepo.getMemos(1).test {
            val memos = awaitItem()
            assertEquals(2, memos.size)
            assertEquals("첫 번째 메모", memos[0].content)
            assertEquals("두 번째 메모", memos[1].content)
            assertEquals(1, memos[0].movieId)
            awaitComplete()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getMemos throws on invalid movieId`() = runTest {
        memoRepo.getMemos(0)
    }

    @Test
    fun `saveMemo delegates to dao insert`() = runTest {
        memoRepo.saveMemo(1, "좋은 영화")

        coVerify { memoDao.insert(match { it.movieId == 1 && it.content == "좋은 영화" }) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `saveMemo throws on blank content`() = runTest {
        memoRepo.saveMemo(1, "   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `saveMemo throws on content exceeding max length`() = runTest {
        val tooLong = "a".repeat(MemoConstants.MAX_LENGTH + 1)
        memoRepo.saveMemo(1, tooLong)
    }

    @Test
    fun `updateMemo delegates to dao updateMemo`() = runTest {
        memoRepo.updateMemo(42L, "수정된 메모")

        coVerify { memoDao.updateMemo(memoId = 42L, content = "수정된 메모", updatedAt = any()) }
    }

    @Test
    fun `deleteMemo delegates to dao deleteMemo`() = runTest {
        memoRepo.deleteMemo(99L)

        coVerify { memoDao.deleteMemo(99L) }
    }

    @Test
    fun `importUserData calls all DAO insert methods`() = runTest {
        val favMovie = BackupMovie(id = 1, title = "F1", posterPath = null, voteAverage = 8.0, overview = "O1")
        val watchMovie = BackupMovie(id = 2, title = "W1", posterPath = null, voteAverage = 7.0, overview = "O2")
        val backup = UserDataBackup(
            favorites = listOf(favMovie),
            watchlist = listOf(watchMovie),
            ratings = listOf(BackupRating(movieId = 1, rating = 4.0f)),
            memos = listOf(BackupMemo(movieId = 1, content = "Note"))
        )

        backupRepo.importUserData(backup)

        coVerify { favoriteMovieDao.insertAll(match { it.size == 1 && it[0].id == 1 }) }
        coVerify { watchlistDao.insertAll(match { it.size == 1 && it[0].id == 2 }) }
        coVerify { userRatingDao.insertAll(match { it.size == 1 && it[0].movieId == 1 }) }
        coVerify { memoDao.insertAll(match { it.size == 1 && it[0].content == "Note" }) }
    }
}
