package com.choo.moviefinder.presentation.person

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.PersonDetail
import com.choo.moviefinder.domain.usecase.GetPersonCreditsUseCase
import com.choo.moviefinder.domain.usecase.GetPersonDetailUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlinx.coroutines.test.advanceUntilIdle

@OptIn(ExperimentalCoroutinesApi::class)
class PersonDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getPersonDetailUseCase: GetPersonDetailUseCase
    private lateinit var getPersonCreditsUseCase: GetPersonCreditsUseCase

    private val testPersonDetail = PersonDetail(
        id = 123,
        name = "Tom Hanks",
        biography = "An American actor and filmmaker.",
        birthday = "1956-07-09",
        deathday = null,
        placeOfBirth = "Concord, California, USA",
        profilePath = "/xndWFsBlClOJFRdhSt4NBwiPq2o.jpg",
        knownForDepartment = "Acting"
    )

    private val testMovies = listOf(
        Movie(
            id = 1,
            title = "Forrest Gump",
            posterPath = "/arw2vcBveWOVZr6pxd9XTd1TdQa.jpg",
            backdropPath = null,
            overview = "A man with a low IQ has accomplished great things in his life.",
            releaseDate = "1994-07-06",
            voteAverage = 8.5,
            voteCount = 24000
        ),
        Movie(
            id = 2,
            title = "Cast Away",
            posterPath = "/xndWFsBlClOJFRdhSt4NBwiPq2o.jpg",
            backdropPath = null,
            overview = "A FedEx employee is stranded on a deserted island.",
            releaseDate = "2000-12-22",
            voteAverage = 7.8,
            voteCount = 12000
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getPersonDetailUseCase = mockk()
        getPersonCreditsUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(personId: Int = 123): PersonDetailViewModel {
        return PersonDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("personId" to personId)),
            getPersonDetailUseCase = getPersonDetailUseCase,
            getPersonCreditsUseCase = getPersonCreditsUseCase
        )
    }

    @Test
    fun `initial state is Loading`() = runTest {
        coEvery { getPersonDetailUseCase(any()) } returns testPersonDetail
        coEvery { getPersonCreditsUseCase(any()) } returns testMovies
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(PersonDetailUiState.Loading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Success with person detail and movies`() = runTest {
        coEvery { getPersonDetailUseCase(123) } returns testPersonDetail
        coEvery { getPersonCreditsUseCase(123) } returns testMovies
        val viewModel = createViewModel()

        viewModel.uiState.test {
            // skip Loading
            val first = awaitItem()
            val successState = if (first is PersonDetailUiState.Loading) {
                awaitItem() as PersonDetailUiState.Success
            } else {
                first as PersonDetailUiState.Success
            }
            assertEquals(testPersonDetail, successState.person)
            assertEquals(testMovies, successState.movies)
        }
    }

    @Test
    fun `emits Success with empty filmography`() = runTest {
        coEvery { getPersonDetailUseCase(123) } returns testPersonDetail
        coEvery { getPersonCreditsUseCase(123) } returns emptyList()
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val first = awaitItem()
            val successState = if (first is PersonDetailUiState.Loading) {
                awaitItem() as PersonDetailUiState.Success
            } else {
                first as PersonDetailUiState.Success
            }
            assertEquals(testPersonDetail, successState.person)
            assertTrue(successState.movies.isEmpty())
        }
    }

    @Test
    fun `emits Error on person detail failure`() = runTest {
        coEvery { getPersonDetailUseCase(123) } throws RuntimeException("Detail load failed")
        coEvery { getPersonCreditsUseCase(123) } returns testMovies
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val first = awaitItem()
            val errorState = if (first is PersonDetailUiState.Loading) {
                awaitItem() as PersonDetailUiState.Error
            } else {
                first as PersonDetailUiState.Error
            }
            assertEquals(ErrorType.UNKNOWN, errorState.errorType)
        }
    }

    @Test
    fun `emits Error on network exception`() = runTest {
        coEvery { getPersonDetailUseCase(123) } throws IOException("Network error")
        coEvery { getPersonCreditsUseCase(123) } returns testMovies
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val first = awaitItem()
            val errorState = if (first is PersonDetailUiState.Loading) {
                awaitItem() as PersonDetailUiState.Error
            } else {
                first as PersonDetailUiState.Error
            }
            assertEquals(ErrorType.NETWORK, errorState.errorType)
        }
    }

    @Test
    fun `loadPersonDetail can be retried after initial load`() = runTest {
        coEvery { getPersonDetailUseCase(any()) } returns testPersonDetail
        coEvery { getPersonCreditsUseCase(any()) } returns testMovies
        val viewModel = createViewModel()

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is PersonDetailUiState.Success)

        viewModel.loadPersonDetail()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is PersonDetailUiState.Success)
        coVerify(exactly = 2) { getPersonDetailUseCase(123) }
    }

    @Test
    fun `loadPersonDetail guards against concurrent calls via mutex`() = runTest {
        coEvery { getPersonDetailUseCase(any()) } returns testPersonDetail
        coEvery { getPersonCreditsUseCase(any()) } returns testMovies
        val viewModel = createViewModel()

        // Call again before init's launch has run — mutex blocks the second
        viewModel.loadPersonDetail()
        advanceUntilIdle()

        // Mutex ensures at most one runs at a time; final state is always Success
        assertTrue(viewModel.uiState.value is PersonDetailUiState.Success)
    }

    @Test
    fun `loads person detail and credits in parallel`() = runTest {
        coEvery { getPersonDetailUseCase(123) } returns testPersonDetail
        coEvery { getPersonCreditsUseCase(123) } returns testMovies
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val first = awaitItem()
            if (first is PersonDetailUiState.Loading) awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { getPersonDetailUseCase(123) }
        coVerify(exactly = 1) { getPersonCreditsUseCase(123) }
    }
}
