package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.BackupMemo
import com.choo.moviefinder.domain.model.BackupMovie
import com.choo.moviefinder.domain.model.BackupRating
import com.choo.moviefinder.domain.model.UserDataBackup
import com.choo.moviefinder.domain.repository.MovieRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ExportImportUseCaseTest {

    private lateinit var repository: MovieRepository
    private lateinit var exportUseCase: ExportUserDataUseCase
    private lateinit var importUseCase: ImportUserDataUseCase

    private val testBackup = UserDataBackup(
        version = 1,
        exportedAt = 1000L,
        favorites = listOf(
            BackupMovie(
                id = 1,
                title = "Favorite Movie",
                posterPath = "/poster.jpg",
                voteAverage = 8.0,
                overview = "Great movie"
            )
        ),
        watchlist = listOf(
            BackupMovie(
                id = 2,
                title = "Watchlist Movie",
                posterPath = null,
                voteAverage = 7.0,
                overview = "Want to watch"
            )
        ),
        ratings = listOf(BackupRating(movieId = 1, rating = 4.5f)),
        memos = listOf(BackupMemo(movieId = 1, content = "Amazing film"))
    )

    @Before
    fun setup() {
        repository = mockk()
        exportUseCase = ExportUserDataUseCase(repository)
        importUseCase = ImportUserDataUseCase(repository)
    }

    @Test
    fun `exportUserData delegates to repository`() = runTest {
        coEvery { repository.exportUserData() } returns testBackup

        exportUseCase()

        coVerify(exactly = 1) { repository.exportUserData() }
    }

    @Test
    fun `exportUserData returns correct backup data`() = runTest {
        coEvery { repository.exportUserData() } returns testBackup

        val result = exportUseCase()

        assertEquals(testBackup, result)
        assertEquals(1, result.favorites.size)
        assertEquals("Favorite Movie", result.favorites[0].title)
        assertEquals(1, result.ratings.size)
        assertEquals(4.5f, result.ratings[0].rating)
        assertEquals(1, result.memos.size)
        assertEquals("Amazing film", result.memos[0].content)
    }

    @Test
    fun `importUserData delegates to repository`() = runTest {
        coEvery { repository.importUserData(any()) } returns Unit

        importUseCase(testBackup)

        coVerify(exactly = 1) { repository.importUserData(any()) }
    }

    @Test
    fun `importUserData passes backup to repository`() = runTest {
        coEvery { repository.importUserData(testBackup) } returns Unit

        importUseCase(testBackup)

        coVerify(exactly = 1) { repository.importUserData(testBackup) }
    }
}
