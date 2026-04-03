package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.Memo
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.PersonDetail
import com.choo.moviefinder.domain.model.PersonSearchItem
import com.choo.moviefinder.domain.repository.MemoRepository
import com.choo.moviefinder.domain.repository.PersonRepository
import com.choo.moviefinder.domain.repository.UserRatingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MemoRatingPersonUseCasesTest {

    private lateinit var memoRepository: MemoRepository
    private lateinit var userRatingRepository: UserRatingRepository
    private lateinit var personRepository: PersonRepository

    private val testMemo = Memo(
        id = 1L,
        movieId = 10,
        content = "Great film",
        createdAt = 1000L,
        updatedAt = 2000L
    )

    private val testPersonDetail = PersonDetail(
        id = 100,
        name = "Test Actor",
        biography = "Bio text",
        birthday = "1980-01-01",
        deathday = null,
        placeOfBirth = "Seoul",
        profilePath = "/profile.jpg",
        knownForDepartment = "Acting"
    )

    private val testMovie = Movie(
        id = 1,
        title = "Film",
        posterPath = null,
        backdropPath = null,
        overview = "Overview",
        releaseDate = "2024-01-01",
        voteAverage = 7.0,
        voteCount = 100
    )

    @Before
    fun setUp() {
        memoRepository = mockk()
        userRatingRepository = mockk()
        personRepository = mockk()
    }

    // --- GetMemosUseCase ---

    @Test
    fun `GetMemosUseCase delegates to repository with correct movieId`() {
        val flow = flowOf(listOf(testMemo))
        every { memoRepository.getMemos(10) } returns flow
        val useCase = GetMemosUseCase(memoRepository)

        val result = useCase(10)

        verify(exactly = 1) { memoRepository.getMemos(10) }
        assertEquals(flow, result)
    }

    @Test
    fun `GetMemosUseCase returns memo list from repository`() = runTest {
        every { memoRepository.getMemos(10) } returns flowOf(listOf(testMemo))
        val useCase = GetMemosUseCase(memoRepository)

        val result = useCase(10).first()

        assertEquals(listOf(testMemo), result)
    }

    // --- SaveMemoUseCase ---

    @Test
    fun `SaveMemoUseCase calls saveMemo with correct arguments`() = runTest {
        coEvery { memoRepository.saveMemo(10, "My note") } returns Unit
        val useCase = SaveMemoUseCase(memoRepository)

        useCase(10, "My note")

        coVerify(exactly = 1) { memoRepository.saveMemo(10, "My note") }
    }

    @Test
    fun `SaveMemoUseCase passes movieId and content correctly`() = runTest {
        val capturedMovieIds = mutableListOf<Int>()
        val capturedContents = mutableListOf<String>()
        coEvery {
            memoRepository.saveMemo(capture(capturedMovieIds), capture(capturedContents))
        } returns Unit
        val useCase = SaveMemoUseCase(memoRepository)

        useCase(42, "Important note")

        assertEquals(42, capturedMovieIds.first())
        assertEquals("Important note", capturedContents.first())
    }

    // --- UpdateMemoUseCase ---

    @Test
    fun `UpdateMemoUseCase calls updateMemo with correct memoId and content`() = runTest {
        coEvery { memoRepository.updateMemo(1L, "Updated note") } returns Unit
        val useCase = UpdateMemoUseCase(memoRepository)

        useCase(1L, "Updated note")

        coVerify(exactly = 1) { memoRepository.updateMemo(1L, "Updated note") }
    }

    @Test
    fun `UpdateMemoUseCase passes correct parameters`() = runTest {
        val capturedIds = mutableListOf<Long>()
        val capturedContents = mutableListOf<String>()
        coEvery {
            memoRepository.updateMemo(capture(capturedIds), capture(capturedContents))
        } returns Unit
        val useCase = UpdateMemoUseCase(memoRepository)

        useCase(99L, "New content")

        assertEquals(99L, capturedIds.first())
        assertEquals("New content", capturedContents.first())
    }

    // --- DeleteMemoUseCase ---

    @Test
    fun `DeleteMemoUseCase calls deleteMemo with correct memoId`() = runTest {
        coEvery { memoRepository.deleteMemo(5L) } returns Unit
        val useCase = DeleteMemoUseCase(memoRepository)

        useCase(5L)

        coVerify(exactly = 1) { memoRepository.deleteMemo(5L) }
    }

    @Test
    fun `DeleteMemoUseCase passes correct memoId`() = runTest {
        val captured = mutableListOf<Long>()
        coEvery { memoRepository.deleteMemo(capture(captured)) } returns Unit
        val useCase = DeleteMemoUseCase(memoRepository)

        useCase(77L)

        assertEquals(77L, captured.first())
    }

    // --- GetUserRatingUseCase ---

    @Test
    fun `GetUserRatingUseCase delegates to repository with correct movieId`() {
        val flow = flowOf(4.5f)
        every { userRatingRepository.getUserRating(10) } returns flow
        val useCase = GetUserRatingUseCase(userRatingRepository)

        val result = useCase(10)

        verify(exactly = 1) { userRatingRepository.getUserRating(10) }
        assertEquals(flow, result)
    }

    @Test
    fun `GetUserRatingUseCase returns rating from repository`() = runTest {
        every { userRatingRepository.getUserRating(10) } returns flowOf(3.5f)
        val useCase = GetUserRatingUseCase(userRatingRepository)

        val result = useCase(10).first()

        assertEquals(3.5f, result)
    }

    // --- SetUserRatingUseCase ---

    @Test
    fun `SetUserRatingUseCase calls setUserRating with correct arguments`() = runTest {
        coEvery { userRatingRepository.setUserRating(10, 4.5f) } returns Unit
        val useCase = SetUserRatingUseCase(userRatingRepository)

        useCase(10, 4.5f)

        coVerify(exactly = 1) { userRatingRepository.setUserRating(10, 4.5f) }
    }

    @Test
    fun `SetUserRatingUseCase passes movieId and rating correctly`() = runTest {
        val capturedIds = mutableListOf<Int>()
        val capturedRatings = mutableListOf<Float>()
        coEvery {
            userRatingRepository.setUserRating(capture(capturedIds), capture(capturedRatings))
        } returns Unit
        val useCase = SetUserRatingUseCase(userRatingRepository)

        useCase(55, 5.0f)

        assertEquals(55, capturedIds.first())
        assertEquals(5.0f, capturedRatings.first())
    }

    // --- DeleteUserRatingUseCase ---

    @Test
    fun `DeleteUserRatingUseCase calls deleteUserRating with correct movieId`() = runTest {
        coEvery { userRatingRepository.deleteUserRating(10) } returns Unit
        val useCase = DeleteUserRatingUseCase(userRatingRepository)

        useCase(10)

        coVerify(exactly = 1) { userRatingRepository.deleteUserRating(10) }
    }

    @Test
    fun `DeleteUserRatingUseCase passes correct movieId`() = runTest {
        val captured = mutableListOf<Int>()
        coEvery { userRatingRepository.deleteUserRating(capture(captured)) } returns Unit
        val useCase = DeleteUserRatingUseCase(userRatingRepository)

        useCase(33)

        assertEquals(33, captured.first())
    }

    // --- GetPersonDetailUseCase ---

    @Test
    fun `GetPersonDetailUseCase returns person detail from repository`() = runTest {
        coEvery { personRepository.getPersonDetail(100) } returns testPersonDetail
        val useCase = GetPersonDetailUseCase(personRepository)

        val result = useCase(100)

        assertEquals(testPersonDetail, result)
    }

    @Test
    fun `GetPersonDetailUseCase passes correct personId`() = runTest {
        coEvery { personRepository.getPersonDetail(200) } returns testPersonDetail
        val useCase = GetPersonDetailUseCase(personRepository)

        useCase(200)

        coVerify(exactly = 1) { personRepository.getPersonDetail(200) }
    }

    // --- GetPersonCreditsUseCase ---

    @Test
    fun `GetPersonCreditsUseCase returns movie list from repository`() = runTest {
        coEvery { personRepository.getPersonMovieCredits(100) } returns listOf(testMovie)
        val useCase = GetPersonCreditsUseCase(personRepository)

        val result = useCase(100)

        assertEquals(listOf(testMovie), result)
    }

    @Test
    fun `GetPersonCreditsUseCase passes correct personId`() = runTest {
        coEvery { personRepository.getPersonMovieCredits(50) } returns emptyList()
        val useCase = GetPersonCreditsUseCase(personRepository)

        useCase(50)

        coVerify(exactly = 1) { personRepository.getPersonMovieCredits(50) }
    }

    // --- SearchPersonUseCase ---

    @Test
    fun `SearchPersonUseCase returns search results from repository`() = runTest {
        val item = PersonSearchItem(1, "Actor", "/p.jpg", "Acting", "Film A")
        coEvery { personRepository.searchPerson("Actor") } returns listOf(item)
        val useCase = SearchPersonUseCase(personRepository)

        val result = useCase("Actor")

        assertEquals(listOf(item), result)
    }

    @Test
    fun `SearchPersonUseCase passes correct query to repository`() = runTest {
        val captured = mutableListOf<String>()
        coEvery { personRepository.searchPerson(capture(captured)) } returns emptyList()
        val useCase = SearchPersonUseCase(personRepository)

        useCase("Chris Evans")

        assertEquals("Chris Evans", captured.first())
    }
}
