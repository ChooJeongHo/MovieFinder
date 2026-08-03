package com.choo.moviefinder.presentation.widget

import com.choo.moviefinder.domain.model.BoxOffice
import com.choo.moviefinder.domain.model.BoxOfficeMovie
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.usecase.GetDailyBoxOfficeWithTmdbMatchUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

// doWork() 자체는 GlanceAppWidgetManager/WorkManager 같은 Android static API에 묶여 JVM 테스트 가치가 낮다.
// 대신 "UseCase 호출 → 스냅샷 변환" 순수 로직인 buildSnapshot을 분리해 이 테스트가 담당한다.
class BoxOfficeWidgetWorkerTest {

    private lateinit var useCase: GetDailyBoxOfficeWithTmdbMatchUseCase

    private fun boxOffice(rank: Int) = BoxOffice(
        rank = rank,
        rankChange = 0,
        isNewEntry = false,
        movieCode = "cd$rank",
        movieName = "영화 $rank",
        openDate = "2024-01-01",
        audienceCount = rank * 100L,
        audienceAccumulate = rank * 1000L,
        salesAmount = rank * 10_000L,
        screenCount = 50
    )

    private fun movie(id: Int) = Movie(id, "영화 $id", null, null, "", "2024-01-01", 7.0, 10)

    @Before
    fun setUp() {
        useCase = mockk()
    }

    @Test
    fun `buildSnapshot converts use case result into top 3 snapshot`() = runTest {
        coEvery { useCase(targetDate = null) } returns (1..10).map {
            BoxOfficeMovie(boxOffice(it), movie(it))
        }

        val snapshot = BoxOfficeWidgetWorker.buildSnapshot(useCase, nowMillis = 42L)

        assertEquals(BoxOfficeWidget.TOP_COUNT, snapshot.items.size)
        assertEquals(listOf(1, 2, 3), snapshot.items.map { it.rank })
        assertEquals(42L, snapshot.fetchedAtMillis)
        coVerify(exactly = 1) { useCase(targetDate = null) }
    }

    @Test
    fun `buildSnapshot returns empty snapshot when use case returns no items`() = runTest {
        coEvery { useCase(targetDate = null) } returns emptyList()

        val snapshot = BoxOfficeWidgetWorker.buildSnapshot(useCase, nowMillis = 7L)

        assertTrue(snapshot.items.isEmpty())
        assertEquals(7L, snapshot.fetchedAtMillis)
    }

    @Test
    fun `buildSnapshot keeps null tmdb id for unmatched entries`() = runTest {
        coEvery { useCase(targetDate = null) } returns listOf(BoxOfficeMovie(boxOffice(1), null))

        val snapshot = BoxOfficeWidgetWorker.buildSnapshot(useCase, nowMillis = 0L)

        assertNull(snapshot.items.single().tmdbId)
    }

    @Test
    fun `buildSnapshot propagates use case failure so doWork can retry`() = runTest {
        coEvery { useCase(targetDate = null) } throws IOException("network down")

        val thrown = runCatching { BoxOfficeWidgetWorker.buildSnapshot(useCase, nowMillis = 0L) }
            .exceptionOrNull()

        assertTrue(thrown is IOException)
    }

    @Test
    fun `unique work names are distinct so periodic and one-time jobs do not cancel each other`() {
        assertEquals("box_office_widget_periodic", BoxOfficeWidgetWorker.PERIODIC_WORK_NAME)
        assertEquals("box_office_widget_refresh", BoxOfficeWidgetWorker.ONE_TIME_WORK_NAME)
        assertTrue(BoxOfficeWidgetWorker.PERIODIC_WORK_NAME != BoxOfficeWidgetWorker.ONE_TIME_WORK_NAME)
    }
}
