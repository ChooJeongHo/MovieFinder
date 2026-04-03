package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.MovieTag
import com.choo.moviefinder.domain.model.ThemeMode
import com.choo.moviefinder.domain.repository.PreferencesRepository
import com.choo.moviefinder.domain.repository.TagRepository
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

class PreferencesTagUseCasesTest {

    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var tagRepository: TagRepository

    private val testMovie = Movie(
        id = 1,
        title = "Tag Movie",
        posterPath = null,
        backdropPath = null,
        overview = "Overview",
        releaseDate = "2024-01-01",
        voteAverage = 7.0,
        voteCount = 100
    )

    private val testTag = MovieTag(
        id = 1L,
        movieId = 10,
        tagName = "action",
        addedAt = 1000L
    )

    @Before
    fun setUp() {
        preferencesRepository = mockk()
        tagRepository = mockk()
    }

    // --- GetThemeModeUseCase ---

    @Test
    fun `GetThemeModeUseCase delegates to repository`() {
        val flow = flowOf(ThemeMode.DARK)
        every { preferencesRepository.getThemeMode() } returns flow
        val useCase = GetThemeModeUseCase(preferencesRepository)

        val result = useCase()

        verify(exactly = 1) { preferencesRepository.getThemeMode() }
        assertEquals(flow, result)
    }

    @Test
    fun `GetThemeModeUseCase returns theme mode from repository`() = runTest {
        every { preferencesRepository.getThemeMode() } returns flowOf(ThemeMode.LIGHT)
        val useCase = GetThemeModeUseCase(preferencesRepository)

        val result = useCase().first()

        assertEquals(ThemeMode.LIGHT, result)
    }

    // --- SetThemeModeUseCase ---

    @Test
    fun `SetThemeModeUseCase calls setThemeMode with correct value`() = runTest {
        coEvery { preferencesRepository.setThemeMode(ThemeMode.DARK) } returns Unit
        val useCase = SetThemeModeUseCase(preferencesRepository)

        useCase(ThemeMode.DARK)

        coVerify(exactly = 1) { preferencesRepository.setThemeMode(ThemeMode.DARK) }
    }

    @Test
    fun `SetThemeModeUseCase passes SYSTEM theme to repository`() = runTest {
        val captured = mutableListOf<ThemeMode>()
        coEvery { preferencesRepository.setThemeMode(capture(captured)) } returns Unit
        val useCase = SetThemeModeUseCase(preferencesRepository)

        useCase(ThemeMode.SYSTEM)

        assertEquals(ThemeMode.SYSTEM, captured.first())
    }

    // --- GetMonthlyWatchGoalUseCase ---

    @Test
    fun `GetMonthlyWatchGoalUseCase delegates to repository`() {
        val flow = flowOf(20)
        every { preferencesRepository.getMonthlyWatchGoal() } returns flow
        val useCase = GetMonthlyWatchGoalUseCase(preferencesRepository)

        val result = useCase()

        verify(exactly = 1) { preferencesRepository.getMonthlyWatchGoal() }
        assertEquals(flow, result)
    }

    @Test
    fun `GetMonthlyWatchGoalUseCase returns goal value from repository`() = runTest {
        every { preferencesRepository.getMonthlyWatchGoal() } returns flowOf(10)
        val useCase = GetMonthlyWatchGoalUseCase(preferencesRepository)

        val result = useCase().first()

        assertEquals(10, result)
    }

    // --- SetMonthlyWatchGoalUseCase ---

    @Test
    fun `SetMonthlyWatchGoalUseCase calls setMonthlyWatchGoal with value 0`() = runTest {
        coEvery { preferencesRepository.setMonthlyWatchGoal(0) } returns Unit
        val useCase = SetMonthlyWatchGoalUseCase(preferencesRepository)

        useCase(0)

        coVerify(exactly = 1) { preferencesRepository.setMonthlyWatchGoal(0) }
    }

    @Test
    fun `SetMonthlyWatchGoalUseCase calls setMonthlyWatchGoal with boundary value 100`() = runTest {
        coEvery { preferencesRepository.setMonthlyWatchGoal(100) } returns Unit
        val useCase = SetMonthlyWatchGoalUseCase(preferencesRepository)

        useCase(100)

        coVerify(exactly = 1) { preferencesRepository.setMonthlyWatchGoal(100) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SetMonthlyWatchGoalUseCase throws IllegalArgumentException for value 101`() = runTest {
        val useCase = SetMonthlyWatchGoalUseCase(preferencesRepository)

        useCase(101)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SetMonthlyWatchGoalUseCase throws IllegalArgumentException for negative value`() = runTest {
        val useCase = SetMonthlyWatchGoalUseCase(preferencesRepository)

        useCase(-1)
    }

    @Test
    fun `SetMonthlyWatchGoalUseCase calls repository with mid-range value`() = runTest {
        coEvery { preferencesRepository.setMonthlyWatchGoal(50) } returns Unit
        val useCase = SetMonthlyWatchGoalUseCase(preferencesRepository)

        useCase(50)

        coVerify(exactly = 1) { preferencesRepository.setMonthlyWatchGoal(50) }
    }

    // --- GetTagsForMovieUseCase ---

    @Test
    fun `GetTagsForMovieUseCase delegates to repository with correct movieId`() {
        val flow = flowOf(listOf(testTag))
        every { tagRepository.getTagsForMovie(10) } returns flow
        val useCase = GetTagsForMovieUseCase(tagRepository)

        val result = useCase(10)

        verify(exactly = 1) { tagRepository.getTagsForMovie(10) }
        assertEquals(flow, result)
    }

    @Test
    fun `GetTagsForMovieUseCase returns tag list from repository`() = runTest {
        every { tagRepository.getTagsForMovie(10) } returns flowOf(listOf(testTag))
        val useCase = GetTagsForMovieUseCase(tagRepository)

        val result = useCase(10).first()

        assertEquals(listOf(testTag), result)
    }

    // --- GetAllTagNamesUseCase ---

    @Test
    fun `GetAllTagNamesUseCase delegates to repository`() {
        val flow = flowOf(listOf("action", "drama"))
        every { tagRepository.getAllTagNames() } returns flow
        val useCase = GetAllTagNamesUseCase(tagRepository)

        val result = useCase()

        verify(exactly = 1) { tagRepository.getAllTagNames() }
        assertEquals(flow, result)
    }

    @Test
    fun `GetAllTagNamesUseCase returns tag names from repository`() = runTest {
        every { tagRepository.getAllTagNames() } returns flowOf(listOf("sci-fi", "horror"))
        val useCase = GetAllTagNamesUseCase(tagRepository)

        val result = useCase().first()

        assertEquals(listOf("sci-fi", "horror"), result)
    }

    // --- AddTagToMovieUseCase ---

    @Test
    fun `AddTagToMovieUseCase calls addTag with correct arguments`() = runTest {
        coEvery { tagRepository.addTag(10, "action") } returns Unit
        val useCase = AddTagToMovieUseCase(tagRepository)

        useCase(10, "action")

        coVerify(exactly = 1) { tagRepository.addTag(10, "action") }
    }

    @Test
    fun `AddTagToMovieUseCase passes movieId and tagName correctly`() = runTest {
        val capturedMovieIds = mutableListOf<Int>()
        val capturedTagNames = mutableListOf<String>()
        coEvery {
            tagRepository.addTag(capture(capturedMovieIds), capture(capturedTagNames))
        } returns Unit
        val useCase = AddTagToMovieUseCase(tagRepository)

        useCase(99, "comedy")

        assertEquals(99, capturedMovieIds.first())
        assertEquals("comedy", capturedTagNames.first())
    }

    // --- RemoveTagFromMovieUseCase ---

    @Test
    fun `RemoveTagFromMovieUseCase calls removeTag with correct arguments`() = runTest {
        coEvery { tagRepository.removeTag(10, "action") } returns Unit
        val useCase = RemoveTagFromMovieUseCase(tagRepository)

        useCase(10, "action")

        coVerify(exactly = 1) { tagRepository.removeTag(10, "action") }
    }

    @Test
    fun `RemoveTagFromMovieUseCase passes movieId and tagName correctly`() = runTest {
        val capturedMovieIds = mutableListOf<Int>()
        val capturedTagNames = mutableListOf<String>()
        coEvery {
            tagRepository.removeTag(capture(capturedMovieIds), capture(capturedTagNames))
        } returns Unit
        val useCase = RemoveTagFromMovieUseCase(tagRepository)

        useCase(7, "thriller")

        assertEquals(7, capturedMovieIds.first())
        assertEquals("thriller", capturedTagNames.first())
    }

    // --- GetFavoritesByTagUseCase ---

    @Test
    fun `GetFavoritesByTagUseCase delegates to repository with correct tagName`() {
        val flow = flowOf(listOf(testMovie))
        every { tagRepository.getFavoritesByTag("action") } returns flow
        val useCase = GetFavoritesByTagUseCase(tagRepository)

        val result = useCase("action")

        verify(exactly = 1) { tagRepository.getFavoritesByTag("action") }
        assertEquals(flow, result)
    }

    @Test
    fun `GetFavoritesByTagUseCase returns movie list from repository`() = runTest {
        every { tagRepository.getFavoritesByTag("drama") } returns flowOf(listOf(testMovie))
        val useCase = GetFavoritesByTagUseCase(tagRepository)

        val result = useCase("drama").first()

        assertEquals(listOf(testMovie), result)
    }
}
