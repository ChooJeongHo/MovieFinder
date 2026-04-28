package com.choo.moviefinder.presentation.detail

import app.cash.turbine.test
import com.choo.moviefinder.core.notification.ReleaseNotificationScheduler
import com.choo.moviefinder.core.notification.WatchGoalNotificationHelper
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.Cast
import com.choo.moviefinder.domain.model.Credits
import com.choo.moviefinder.domain.model.Genre
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.MovieDetail
import com.choo.moviefinder.domain.model.Review
import com.choo.moviefinder.domain.usecase.GetMovieCertificationUseCase
import com.choo.moviefinder.domain.usecase.GetMovieCreditsUseCase
import com.choo.moviefinder.domain.usecase.GetMovieDetailUseCase
import com.choo.moviefinder.domain.usecase.GetMovieRecommendationsUseCase
import com.choo.moviefinder.domain.usecase.GetMovieReviewsUseCase
import com.choo.moviefinder.domain.usecase.GetMovieTrailerUseCase
import com.choo.moviefinder.domain.usecase.GetSimilarMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetWatchProvidersUseCase
import com.choo.moviefinder.domain.usecase.IsFavoriteUseCase
import com.choo.moviefinder.domain.usecase.IsInWatchlistUseCase
import com.choo.moviefinder.domain.usecase.SaveWatchHistoryUseCase
import com.choo.moviefinder.domain.usecase.GetTmdbAccessTokenUseCase
import com.choo.moviefinder.domain.usecase.SubmitTmdbRatingUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException
import androidx.lifecycle.SavedStateHandle

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getMovieDetailUseCase: GetMovieDetailUseCase
    private lateinit var getMovieCreditsUseCase: GetMovieCreditsUseCase
    private lateinit var getSimilarMoviesUseCase: GetSimilarMoviesUseCase
    private lateinit var getMovieTrailerUseCase: GetMovieTrailerUseCase
    private lateinit var getMovieCertificationUseCase: GetMovieCertificationUseCase
    private lateinit var getMovieReviewsUseCase: GetMovieReviewsUseCase
    private lateinit var getMovieRecommendationsUseCase: GetMovieRecommendationsUseCase
    private lateinit var getWatchProvidersUseCase: GetWatchProvidersUseCase
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase
    private lateinit var isFavoriteUseCase: IsFavoriteUseCase
    private lateinit var toggleWatchlistUseCase: ToggleWatchlistUseCase
    private lateinit var isInWatchlistUseCase: IsInWatchlistUseCase
    private lateinit var saveWatchHistoryUseCase: SaveWatchHistoryUseCase
    private lateinit var getUserRatingUseCase: com.choo.moviefinder.domain.usecase.GetUserRatingUseCase
    private lateinit var setUserRatingUseCase: com.choo.moviefinder.domain.usecase.SetUserRatingUseCase
    private lateinit var deleteUserRatingUseCase: com.choo.moviefinder.domain.usecase.DeleteUserRatingUseCase
    private lateinit var getMemosUseCase: com.choo.moviefinder.domain.usecase.GetMemosUseCase
    private lateinit var saveMemoUseCase: com.choo.moviefinder.domain.usecase.SaveMemoUseCase
    private lateinit var updateMemoUseCase: com.choo.moviefinder.domain.usecase.UpdateMemoUseCase
    private lateinit var deleteMemoUseCase: com.choo.moviefinder.domain.usecase.DeleteMemoUseCase
    private lateinit var releaseNotificationScheduler: ReleaseNotificationScheduler
    private lateinit var watchGoalNotificationHelper: WatchGoalNotificationHelper
    private lateinit var getTmdbAccessTokenUseCase: GetTmdbAccessTokenUseCase
    private lateinit var submitTmdbRatingUseCase: SubmitTmdbRatingUseCase

    private val testMovieDetail = MovieDetail(
        id = 1,
        title = "Test Movie",
        posterPath = "/poster.jpg",
        backdropPath = "/backdrop.jpg",
        overview = "Test overview",
        releaseDate = "2024-01-01",
        voteAverage = 8.5,
        voteCount = 1000,
        runtime = 120,
        genres = listOf(Genre(1, "Action")),
        tagline = "Test tagline"
    )

    private val testCasts = listOf(
        Cast(1, "Actor 1", "Character 1", "/profile1.jpg"),
        Cast(2, "Actor 2", "Character 2", "/profile2.jpg")
    )

    private val testCredits = Credits(cast = testCasts, directors = listOf("Test Director"))

    private val testSimilarMovies = listOf(
        Movie(2, "Similar 1", "/poster2.jpg", "/backdrop2.jpg", "Overview 2", "2024-02-01", 7.0, 500),
        Movie(3, "Similar 2", "/poster3.jpg", "/backdrop3.jpg", "Overview 3", "2024-03-01", 6.5, 300)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getMovieDetailUseCase = mockk()
        getMovieCreditsUseCase = mockk()
        getSimilarMoviesUseCase = mockk()
        getMovieTrailerUseCase = mockk()
        getMovieCertificationUseCase = mockk()
        getMovieReviewsUseCase = mockk()
        toggleFavoriteUseCase = mockk()
        isFavoriteUseCase = mockk()
        toggleWatchlistUseCase = mockk()
        isInWatchlistUseCase = mockk()
        saveWatchHistoryUseCase = mockk()
        getUserRatingUseCase = mockk()
        setUserRatingUseCase = mockk()
        deleteUserRatingUseCase = mockk()
        getMemosUseCase = mockk()
        saveMemoUseCase = mockk()
        updateMemoUseCase = mockk()
        deleteMemoUseCase = mockk()
        releaseNotificationScheduler = mockk(relaxed = true)
        watchGoalNotificationHelper = mockk(relaxed = true)
        getTmdbAccessTokenUseCase = mockk()
        submitTmdbRatingUseCase = mockk()
        every { getTmdbAccessTokenUseCase() } returns flowOf(null)

        every { getMemosUseCase(any()) } returns flowOf(emptyList())

        every { getUserRatingUseCase(any()) } returns flowOf(null)

        getMovieRecommendationsUseCase = mockk()
        getWatchProvidersUseCase = mockk()

        coEvery { getMovieTrailerUseCase(any()) } returns null
        coEvery { getMovieCertificationUseCase(any()) } returns null
        coEvery { getMovieReviewsUseCase(any()) } returns emptyList()
        coEvery { getMovieRecommendationsUseCase(any()) } returns emptyList()
        coEvery { getWatchProvidersUseCase(any()) } returns emptyList()
        coEvery { saveWatchHistoryUseCase(any(), any()) } returns Unit

        every { isFavoriteUseCase(any()) } returns flowOf(false)
        every { isInWatchlistUseCase(any()) } returns flowOf(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(movieId: Int = 1): DetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("movieId" to movieId))
        val fetch = DetailFetchUseCases(
            getMovieDetail = getMovieDetailUseCase,
            getMovieCredits = getMovieCreditsUseCase,
            getSimilarMovies = getSimilarMoviesUseCase,
            getMovieTrailer = getMovieTrailerUseCase,
            getMovieCertification = getMovieCertificationUseCase,
            getMovieReviews = getMovieReviewsUseCase,
            getMovieRecommendations = getMovieRecommendationsUseCase,
            getWatchProviders = getWatchProvidersUseCase
        )
        val toggle = DetailToggleUseCases(
            toggleFavorite = toggleFavoriteUseCase,
            isFavorite = isFavoriteUseCase,
            toggleWatchlist = toggleWatchlistUseCase,
            isInWatchlist = isInWatchlistUseCase
        )
        val memo = DetailMemoUseCases(
            getMemos = getMemosUseCase,
            saveMemo = saveMemoUseCase,
            updateMemo = updateMemoUseCase,
            deleteMemo = deleteMemoUseCase
        )
        val ratingCases = DetailRatingUseCases(
            getUserRating = getUserRatingUseCase,
            setUserRating = setUserRatingUseCase,
            deleteUserRating = deleteUserRatingUseCase,
            getTmdbAccessToken = getTmdbAccessTokenUseCase,
            submitTmdbRating = submitTmdbRatingUseCase
        )
        return DetailViewModel(
            savedStateHandle = savedStateHandle,
            fetch = fetch,
            toggle = toggle,
            memo = memo,
            ratingCases = ratingCases,
            saveWatchHistoryUseCase = saveWatchHistoryUseCase,
            releaseNotificationScheduler = releaseNotificationScheduler,
            watchGoalNotificationHelper = watchGoalNotificationHelper
        )
    }

    @Test
    fun `init triggers loading then success state`() = runTest {
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DetailUiState.Success)
        state as DetailUiState.Success
        assertEquals(testMovieDetail, state.movieDetail)
        assertEquals(testCredits, state.credits)
        assertEquals(testSimilarMovies, state.similarMovies)
    }

    @Test
    fun `detail API failure shows error state with NETWORK type`() = runTest {
        val exception = UnknownHostException("no network")
        coEvery { getMovieDetailUseCase(1) } throws exception
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DetailUiState.Error)
        assertEquals(ErrorType.NETWORK, (state as DetailUiState.Error).errorType)
    }

    @Test
    fun `credits failure still shows success with empty credits`() = runTest {
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } throws RuntimeException("credits failed")
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DetailUiState.Success)
        state as DetailUiState.Success
        assertEquals(testMovieDetail, state.movieDetail)
        assertNull(state.credits)
        assertEquals(testSimilarMovies, state.similarMovies)
    }

    @Test
    fun `similar movies failure still shows success with empty similar`() = runTest {
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } throws RuntimeException("similar failed")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DetailUiState.Success)
        state as DetailUiState.Success
        assertTrue(state.similarMovies!!.isEmpty())
        assertEquals(testCredits, state.credits)
    }

    @Test
    fun `toggleFavorite calls use case with converted movie`() = runTest {
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies
        coEvery { toggleFavoriteUseCase(any()) } returns Unit

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        coVerify {
            toggleFavoriteUseCase(match { movie ->
                movie.id == testMovieDetail.id && movie.title == testMovieDetail.title
            })
        }
    }

    @Test
    fun `toggleFavorite failure emits snackbar event with error type`() = runTest {
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies
        coEvery { toggleFavoriteUseCase(any()) } throws RuntimeException("DB error")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.snackbarEvent.test {
            viewModel.toggleFavorite()
            assertEquals(ErrorType.UNKNOWN, awaitItem())
        }
    }

    @Test
    fun `toggleFavorite does nothing when state is not Success`() = runTest {
        coEvery { getMovieDetailUseCase(1) } throws RuntimeException("fail")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        coVerify(exactly = 0) { toggleFavoriteUseCase(any()) }
    }

    @Test
    fun `isFavorite reflects use case flow`() = runTest {
        every { isFavoriteUseCase(1) } returns flowOf(true)
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies

        val viewModel = createViewModel()

        viewModel.isFavorite.test {
            // stateIn 초기값 false → true
            val first = awaitItem()
            if (!first) {
                assertEquals(true, awaitItem())
            } else {
                assertEquals(true, first)
            }
        }
    }

    @Test
    fun `trailer failure still shows success with null trailer`() = runTest {
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies
        coEvery { getMovieTrailerUseCase(1) } throws RuntimeException("trailer failed")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DetailUiState.Success)
        state as DetailUiState.Success
        assertEquals(null, state.trailerKey)
        assertEquals(testMovieDetail, state.movieDetail)
    }

    @Test
    fun `loadMovieDetail ignores duplicate call while loading`() = runTest {
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies

        val viewModel = createViewModel()
        // init sets Loading, calling again should be ignored
        viewModel.loadMovieDetail()
        advanceUntilIdle()

        // Detail API should be called only once (from init)
        coVerify(exactly = 1) { getMovieDetailUseCase(1) }
    }

    @Test
    fun `loadMovieDetail can be retried after error`() = runTest {
        coEvery { getMovieDetailUseCase(1) } throws RuntimeException("fail")

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is DetailUiState.Error)

        // 재시도 시 성공
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies

        viewModel.loadMovieDetail()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DetailUiState.Success)
    }

    @Test
    fun `toggleWatchlist calls use case with converted movie`() = runTest {
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies
        coEvery { toggleWatchlistUseCase(any()) } returns Unit

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleWatchlist()
        advanceUntilIdle()

        coVerify {
            toggleWatchlistUseCase(match { movie ->
                movie.id == testMovieDetail.id && movie.title == testMovieDetail.title
            })
        }
    }

    @Test
    fun `toggleWatchlist failure emits snackbar event`() = runTest {
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies
        coEvery { toggleWatchlistUseCase(any()) } throws RuntimeException("DB error")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.snackbarEvent.test {
            viewModel.toggleWatchlist()
            assertEquals(ErrorType.UNKNOWN, awaitItem())
        }
    }

    @Test
    fun `isInWatchlist reflects use case flow`() = runTest {
        every { isInWatchlistUseCase(1) } returns flowOf(true)
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies

        val viewModel = createViewModel()

        viewModel.isInWatchlist.test {
            val first = awaitItem()
            if (!first) {
                assertEquals(true, awaitItem())
            } else {
                assertEquals(true, first)
            }
        }
    }

    @Test
    fun `certification loads in success state`() = runTest {
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies
        coEvery { getMovieCertificationUseCase(1) } returns "15"

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DetailUiState.Success)
        state as DetailUiState.Success
        assertEquals("15", state.certification)
    }

    @Test
    fun `reviews load in success state`() = runTest {
        val testReviews = listOf(
            Review("r1", "Author1", null, 8.0, "Great movie!", "2024-01-01"),
            Review("r2", "Author2", "/avatar.jpg", null, "Not bad", "2024-02-01")
        )
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies
        coEvery { getMovieReviewsUseCase(1) } returns testReviews

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DetailUiState.Success)
        state as DetailUiState.Success
        assertEquals(2, state.reviews!!.size)
        assertEquals("Author1", state.reviews!![0].author)
    }

    @Test
    fun `reviews failure still shows success with empty reviews`() = runTest {
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies
        coEvery { getMovieReviewsUseCase(1) } throws RuntimeException("reviews failed")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DetailUiState.Success)
        state as DetailUiState.Success
        assertTrue(state.reviews!!.isEmpty())
    }

    @Test
    fun `toggleWatchlist schedules notification when adding to watchlist`() = runTest {
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies
        coEvery { toggleWatchlistUseCase(any()) } returns Unit
        every { isInWatchlistUseCase(1) } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleWatchlist()
        advanceUntilIdle()

        io.mockk.verify {
            releaseNotificationScheduler.schedule(
                movieId = 1,
                movieTitle = "Test Movie",
                releaseDate = "2024-01-01"
            )
        }
    }

    // --- User Rating ---

    @Test
    fun `setUserRating calls use case with correct params`() = runTest {
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies
        coEvery { setUserRatingUseCase(any(), any()) } returns Unit

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setUserRating(4.5f)
        advanceUntilIdle()

        coVerify { setUserRatingUseCase(1, 4.5f) }
    }

    @Test
    fun `setUserRating failure emits snackbar event`() = runTest {
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies
        coEvery { setUserRatingUseCase(any(), any()) } throws RuntimeException("DB error")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.snackbarEvent.test {
            viewModel.setUserRating(3.0f)
            assertEquals(ErrorType.UNKNOWN, awaitItem())
        }
    }

    @Test
    fun `deleteUserRating calls use case`() = runTest {
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies
        coEvery { deleteUserRatingUseCase(any()) } returns Unit

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.deleteUserRating()
        advanceUntilIdle()

        coVerify { deleteUserRatingUseCase(1) }
    }

    @Test
    fun `deleteUserRating failure emits snackbar event`() = runTest {
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies
        coEvery { deleteUserRatingUseCase(any()) } throws RuntimeException("DB error")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.snackbarEvent.test {
            viewModel.deleteUserRating()
            assertEquals(ErrorType.UNKNOWN, awaitItem())
        }
    }

    @Test
    fun `userRating emits values from use case`() = runTest {
        every { getUserRatingUseCase(1) } returns flowOf(4.0f)
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies

        val viewModel = createViewModel()

        viewModel.userRating.test {
            val first = awaitItem()
            if (first == null) {
                assertEquals(4.0f, awaitItem())
            } else {
                assertEquals(4.0f, first)
            }
        }
    }

    @Test
    fun `toggleWatchlist cancels notification when removing from watchlist`() = runTest {
        every { isInWatchlistUseCase(1) } returns flowOf(true)
        coEvery { getMovieDetailUseCase(1) } returns testMovieDetail
        coEvery { getMovieCreditsUseCase(1) } returns testCredits
        coEvery { getSimilarMoviesUseCase(1) } returns testSimilarMovies
        coEvery { toggleWatchlistUseCase(any()) } returns Unit

        val viewModel = createViewModel()
        // stateIn(WhileSubscribed)은 구독자가 있어야 수집 시작
        viewModel.isInWatchlist.test {
            // 초기값 false → true
            skipItems(1)
            assertEquals(true, awaitItem())
        }

        viewModel.toggleWatchlist()
        advanceUntilIdle()

        io.mockk.verify { releaseNotificationScheduler.cancel(1) }
    }
}
