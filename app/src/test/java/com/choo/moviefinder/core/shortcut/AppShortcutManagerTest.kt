package com.choo.moviefinder.core.shortcut

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.usecase.GetRecentShortcutMoviesUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppShortcutManagerTest {

    private lateinit var context: Context
    private lateinit var getRecentShortcutMovies: GetRecentShortcutMoviesUseCase
    private lateinit var manager: AppShortcutManager

    private fun movie(id: Int, title: String = "Movie $id") = Movie(
        id = id,
        title = title,
        posterPath = "/poster$id.jpg",
        backdropPath = null,
        overview = "Overview $id",
        releaseDate = "2024-01-01",
        voteAverage = 7.0,
        voteCount = 100
    )

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        every { context.packageName } returns "com.choo.moviefinder"

        getRecentShortcutMovies = mockk()

        mockkStatic(ShortcutManagerCompat::class)
        every { ShortcutManagerCompat.setDynamicShortcuts(any(), any()) } returns true

        // android.content.Intent 메서드는 JVM 유닛 테스트에서 항상 기본값(null 등)을 반환하는 스텁이라
        // 실제 getter로 값을 되읽을 수 없다 (isReturnDefaultValues=true). 생성자 호출 자체와
        // setPackage/setFlags 호출을 mockkConstructor로 가로채 검증 가능하게 만든다.
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().setPackage(any()) } returns mockk(relaxed = true)
        every { anyConstructed<Intent>().setFlags(any()) } returns mockk(relaxed = true)

        manager = AppShortcutManager(context, getRecentShortcutMovies)
    }

    @After
    fun tearDown() {
        unmockkStatic(ShortcutManagerCompat::class)
        unmockkConstructor(Intent::class)
    }

    @Test
    fun `refreshRecentMovieShortcuts sets at most two dynamic shortcuts`() = runTest {
        coEvery { getRecentShortcutMovies() } returns listOf(movie(1), movie(2))
        val listSlot = slot<List<ShortcutInfoCompat>>()
        every { ShortcutManagerCompat.setDynamicShortcuts(any(), capture(listSlot)) } returns true

        manager.refreshRecentMovieShortcuts()

        assertTrue(listSlot.captured.size <= 2)
        assertEquals(2, listSlot.captured.size)
    }

    @Test
    fun `refreshRecentMovieShortcuts sets empty list when no recent movies`() = runTest {
        coEvery { getRecentShortcutMovies() } returns emptyList()
        val listSlot = slot<List<ShortcutInfoCompat>>()
        every { ShortcutManagerCompat.setDynamicShortcuts(any(), capture(listSlot)) } returns true

        manager.refreshRecentMovieShortcuts()

        assertTrue(listSlot.captured.isEmpty())
    }

    @Test
    fun `buildMovieShortcut uses recent_movie prefix as shortcut id`() {
        val shortcut = manager.buildMovieShortcut(movie(42))

        assertEquals("recent_movie_42", shortcut.id)
    }

    @Test
    fun `buildMovieShortcut builds a deep link intent to the movie detail screen`() {
        mockkStatic(Uri::class)
        val testUri = mockk<Uri>(relaxed = true)
        every { Uri.parse("moviefinder://movie/42") } returns testUri

        manager.buildMovieShortcut(movie(42))

        // android.content.Intent의 getter는 JVM 유닛 테스트에서 항상 기본값(null)을 반환하는 스텁이라
        // 되읽어 검증할 수 없다 (isReturnDefaultValues=true). 대신 정확히 어떤 딥링크 문자열로
        // Uri.parse가 호출되었는지(영화 42의 상세 화면 경로), 그리고 setPackage가 올바른 패키지로
        // 호출되었는지를 검증해 action=ACTION_VIEW/data 구성이 올바른 입력으로 이루어졌음을 확인한다.
        verify(exactly = 1) { Uri.parse("moviefinder://movie/42") }
        verify(exactly = 1) { anyConstructed<Intent>().setPackage("com.choo.moviefinder") }

        unmockkStatic(Uri::class)
    }
}
