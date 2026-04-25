package com.choo.moviefinder.presentation.favorite

import android.content.Context
import app.cash.turbine.test
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.core.util.PosterTagSuggester
import com.choo.moviefinder.domain.model.FavoriteSortOrder
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.UserRatingRepository
import com.choo.moviefinder.domain.usecase.AddTagToMovieUseCase
import com.choo.moviefinder.domain.usecase.ClearWatchlistReminderUseCase
import com.choo.moviefinder.domain.usecase.GetAllTagNamesUseCase
import com.choo.moviefinder.domain.usecase.GetFavoriteMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetFavoritesByTagUseCase
import com.choo.moviefinder.domain.usecase.GetTagsForMovieUseCase
import com.choo.moviefinder.domain.usecase.GetWatchlistRemindersUseCase
import com.choo.moviefinder.domain.usecase.GetWatchlistUseCase
import com.choo.moviefinder.domain.usecase.RemoveTagFromMovieUseCase
import com.choo.moviefinder.domain.usecase.SetWatchlistReminderUseCase
import com.choo.moviefinder.domain.usecase.ToggleFavoriteUseCase
import com.choo.moviefinder.domain.usecase.ToggleWatchlistUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getFavoriteMoviesUseCase: GetFavoriteMoviesUseCase
    private lateinit var getFavoritesByTagUseCase: GetFavoritesByTagUseCase
    private lateinit var getWatchlistUseCase: GetWatchlistUseCase
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase
    private lateinit var toggleWatchlistUseCase: ToggleWatchlistUseCase
    private lateinit var getTagsForMovieUseCase: GetTagsForMovieUseCase
    private lateinit var getAllTagNamesUseCase: GetAllTagNamesUseCase
    private lateinit var addTagToMovieUseCase: AddTagToMovieUseCase
    private lateinit var removeTagFromMovieUseCase: RemoveTagFromMovieUseCase
    private lateinit var posterTagSuggester: PosterTagSuggester
    private lateinit var userRatingRepository: UserRatingRepository
    private lateinit var setWatchlistReminderUseCase: SetWatchlistReminderUseCase
    private lateinit var clearWatchlistReminderUseCase: ClearWatchlistReminderUseCase
    private lateinit var getWatchlistRemindersUseCase: GetWatchlistRemindersUseCase
    private lateinit var context: Context

    private val testMovies = listOf(
        Movie(1, "Movie 1", "/poster1.jpg", "/backdrop1.jpg", "Overview 1", "2024-01-01", 8.0, 500),
        Movie(2, "Movie 2", "/poster2.jpg", "/backdrop2.jpg", "Overview 2", "2024-02-01", 7.5, 300)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getFavoriteMoviesUseCase = mockk()
        getFavoritesByTagUseCase = mockk()
        getWatchlistUseCase = mockk()
        toggleFavoriteUseCase = mockk()
        toggleWatchlistUseCase = mockk()
        getTagsForMovieUseCase = mockk()
        getAllTagNamesUseCase = mockk()
        addTagToMovieUseCase = mockk()
        removeTagFromMovieUseCase = mockk()
        posterTagSuggester = mockk()
        userRatingRepository = mockk()
        setWatchlistReminderUseCase = mockk()
        clearWatchlistReminderUseCase = mockk()
        getWatchlistRemindersUseCase = mockk()
        context = mockk(relaxed = true)

        every { getWatchlistUseCase(any<FavoriteSortOrder>()) } returns flowOf(emptyList())
        every { getAllTagNamesUseCase() } returns flowOf(emptyList())
        every { userRatingRepository.getAllUserRatings() } returns flowOf(emptyMap())
        coEvery { getWatchlistRemindersUseCase() } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): FavoriteViewModel {
        return FavoriteViewModel(
            getFavoriteMoviesUseCase,
            getFavoritesByTagUseCase,
            getWatchlistUseCase,
            toggleFavoriteUseCase,
            toggleWatchlistUseCase,
            getTagsForMovieUseCase,
            getAllTagNamesUseCase,
            addTagToMovieUseCase,
            removeTagFromMovieUseCase,
            posterTagSuggester,
            userRatingRepository,
            setWatchlistReminderUseCase,
            clearWatchlistReminderUseCase,
            getWatchlistRemindersUseCase,
            context
        )
    }

    @Test
    fun `favoriteMovies emits movie list from use case`() = runTest {
        every { getFavoriteMoviesUseCase(any<FavoriteSortOrder>()) } returns flowOf(testMovies)

        val viewModel = createViewModel()

        viewModel.favoriteMovies.test {
            val item = awaitItem()
            if (item.isEmpty()) {
                assertEquals(testMovies, awaitItem())
            } else {
                assertEquals(testMovies, item)
            }
        }
    }

    @Test
    fun `favoriteMovies emits empty list when no favorites`() = runTest {
        every { getFavoriteMoviesUseCase(any<FavoriteSortOrder>()) } returns flowOf(emptyList())

        val viewModel = createViewModel()

        viewModel.favoriteMovies.test {
            val item = awaitItem()
            assertTrue(item.isEmpty())
        }
    }

    @Test
    fun `toggleFavorite calls toggle use case`() = runTest {
        every { getFavoriteMoviesUseCase(any<FavoriteSortOrder>()) } returns flowOf(testMovies)
        coEvery { toggleFavoriteUseCase(any()) } returns Unit

        val viewModel = createViewModel()

        viewModel.toggleFavorite(testMovies[0])
        advanceUntilIdle()

        coVerify { toggleFavoriteUseCase(testMovies[0]) }
    }

    @Test
    fun `toggleFavorite does not crash on error`() = runTest {
        every { getFavoriteMoviesUseCase(any<FavoriteSortOrder>()) } returns flowOf(testMovies)
        coEvery { toggleFavoriteUseCase(any()) } throws RuntimeException("DB error")

        val viewModel = createViewModel()

        viewModel.toggleFavorite(testMovies[0])
        advanceUntilIdle()

        coVerify { toggleFavoriteUseCase(testMovies[0]) }
    }

    @Test
    fun `watchlistMovies emits movie list from use case`() = runTest {
        every { getFavoriteMoviesUseCase(any<FavoriteSortOrder>()) } returns flowOf(emptyList())
        every { getWatchlistUseCase(any<FavoriteSortOrder>()) } returns flowOf(testMovies)

        val viewModel = createViewModel()

        viewModel.watchlistMovies.test {
            val item = awaitItem()
            if (item.isEmpty()) {
                assertEquals(testMovies, awaitItem())
            } else {
                assertEquals(testMovies, item)
            }
        }
    }

    @Test
    fun `toggleWatchlist calls use case and handles error`() = runTest {
        every { getFavoriteMoviesUseCase(any<FavoriteSortOrder>()) } returns flowOf(testMovies)
        coEvery { toggleWatchlistUseCase(any()) } returns Unit

        val viewModel = createViewModel()

        viewModel.toggleWatchlist(testMovies[0])
        advanceUntilIdle()

        coVerify { toggleWatchlistUseCase(testMovies[0]) }

        // Also verify error handling
        coEvery { toggleWatchlistUseCase(any()) } throws RuntimeException("DB error")
        viewModel.toggleWatchlist(testMovies[1])
        advanceUntilIdle()

        coVerify { toggleWatchlistUseCase(testMovies[1]) }
    }

    @Test
    fun `toggleFavorite error sends snackbar event`() = runTest {
        every { getFavoriteMoviesUseCase(any<FavoriteSortOrder>()) } returns flowOf(testMovies)
        coEvery { toggleFavoriteUseCase(any()) } throws RuntimeException("DB error")

        val viewModel = createViewModel()

        viewModel.snackbarEvent.test {
            viewModel.toggleFavorite(testMovies[0])
            advanceUntilIdle()

            val errorType = awaitItem()
            assertEquals(ErrorType.UNKNOWN, errorType)
        }
    }

    @Test
    fun `onSortOrderSelected changes sort order`() = runTest {
        every { getFavoriteMoviesUseCase(any<FavoriteSortOrder>()) } returns flowOf(testMovies)

        val viewModel = createViewModel()

        assertEquals(FavoriteSortOrder.ADDED_DATE, viewModel.sortOrder.value)

        viewModel.onSortOrderSelected(FavoriteSortOrder.TITLE)
        assertEquals(FavoriteSortOrder.TITLE, viewModel.sortOrder.value)
    }

    @Test
    fun `favoriteMovies sorted by title`() = runTest {
        val movies = listOf(
            Movie(1, "Zorro", "/p1.jpg", "/b1.jpg", "O1", "2024-01-01", 8.0, 500),
            Movie(2, "Avengers", "/p2.jpg", "/b2.jpg", "O2", "2024-02-01", 7.5, 300)
        )
        val sortedMovies = listOf(movies[1], movies[0]) // Avengers first
        every { getFavoriteMoviesUseCase(FavoriteSortOrder.ADDED_DATE) } returns flowOf(movies)
        every { getFavoriteMoviesUseCase(FavoriteSortOrder.TITLE) } returns flowOf(sortedMovies)

        val viewModel = createViewModel()
        viewModel.onSortOrderSelected(FavoriteSortOrder.TITLE)

        viewModel.favoriteMovies.test {
            val item = awaitItem()
            if (item.isEmpty() || item == movies) {
                val sorted = awaitItem()
                assertEquals("Avengers", sorted[0].title)
                assertEquals("Zorro", sorted[1].title)
            } else {
                assertEquals("Avengers", item[0].title)
                assertEquals("Zorro", item[1].title)
            }
        }
    }

    @Test
    fun `favoriteMovies sorted by rating`() = runTest {
        val movies = listOf(
            Movie(1, "Movie A", "/p1.jpg", "/b1.jpg", "O1", "2024-01-01", 6.0, 500),
            Movie(2, "Movie B", "/p2.jpg", "/b2.jpg", "O2", "2024-02-01", 9.0, 300)
        )
        val sortedMovies = listOf(movies[1], movies[0]) // Movie B (9.0) first
        every { getFavoriteMoviesUseCase(FavoriteSortOrder.ADDED_DATE) } returns flowOf(movies)
        every { getFavoriteMoviesUseCase(FavoriteSortOrder.RATING) } returns flowOf(sortedMovies)

        val viewModel = createViewModel()
        viewModel.onSortOrderSelected(FavoriteSortOrder.RATING)

        viewModel.favoriteMovies.test {
            val item = awaitItem()
            if (item.isEmpty() || item == movies) {
                val sorted = awaitItem()
                assertEquals("Movie B", sorted[0].title)
                assertEquals("Movie A", sorted[1].title)
            } else {
                assertEquals("Movie B", item[0].title)
                assertEquals("Movie A", item[1].title)
            }
        }
    }

    @Test
    fun `addTagToMovie calls use case`() = runTest {
        every { getFavoriteMoviesUseCase(any<FavoriteSortOrder>()) } returns flowOf(testMovies)
        coEvery { addTagToMovieUseCase(any(), any()) } returns Unit

        val viewModel = createViewModel()

        viewModel.addTagToMovie(1, "Action")
        advanceUntilIdle()

        coVerify { addTagToMovieUseCase(1, "Action") }
    }

    @Test
    fun `addTagToMovie error sends snackbar event`() = runTest {
        every { getFavoriteMoviesUseCase(any<FavoriteSortOrder>()) } returns flowOf(testMovies)
        coEvery { addTagToMovieUseCase(any(), any()) } throws RuntimeException("DB error")

        val viewModel = createViewModel()

        viewModel.snackbarEvent.test {
            viewModel.addTagToMovie(1, "Action")
            advanceUntilIdle()

            val errorType = awaitItem()
            assertEquals(ErrorType.UNKNOWN, errorType)
        }
    }

    @Test
    fun `removeTagFromMovie calls use case`() = runTest {
        every { getFavoriteMoviesUseCase(any<FavoriteSortOrder>()) } returns flowOf(testMovies)
        coEvery { removeTagFromMovieUseCase(any(), any()) } returns Unit

        val viewModel = createViewModel()

        viewModel.removeTagFromMovie(1, "Action")
        advanceUntilIdle()

        coVerify { removeTagFromMovieUseCase(1, "Action") }
    }

    @Test
    fun `removeTagFromMovie error sends snackbar event`() = runTest {
        every { getFavoriteMoviesUseCase(any<FavoriteSortOrder>()) } returns flowOf(testMovies)
        coEvery { removeTagFromMovieUseCase(any(), any()) } throws RuntimeException("DB error")

        val viewModel = createViewModel()

        viewModel.snackbarEvent.test {
            viewModel.removeTagFromMovie(1, "Action")
            advanceUntilIdle()

            val errorType = awaitItem()
            assertEquals(ErrorType.UNKNOWN, errorType)
        }
    }

    @Test
    fun `onTagSelected changes selectedTag state`() = runTest {
        every { getFavoriteMoviesUseCase(any<FavoriteSortOrder>()) } returns flowOf(testMovies)
        every { getFavoritesByTagUseCase(any(), any<FavoriteSortOrder>()) } returns flowOf(testMovies)

        val viewModel = createViewModel()

        assertEquals(null, viewModel.selectedTag.value)

        viewModel.onTagSelected("Action")
        advanceUntilIdle()
        assertEquals("Action", viewModel.selectedTag.value)

        viewModel.onTagSelected(null)
        advanceUntilIdle()
        assertEquals(null, viewModel.selectedTag.value)
    }

    @Test
    fun `allTagNames emits tag names from use case`() = runTest {
        every { getFavoriteMoviesUseCase(any<FavoriteSortOrder>()) } returns flowOf(testMovies)
        val tagNames = listOf("Action", "Drama", "Comedy")
        every { getAllTagNamesUseCase() } returns flowOf(tagNames)

        val viewModel = createViewModel()

        viewModel.allTagNames.test {
            val item = awaitItem()
            if (item.isEmpty()) {
                assertEquals(tagNames, awaitItem())
            } else {
                assertEquals(tagNames, item)
            }
        }
    }
}
