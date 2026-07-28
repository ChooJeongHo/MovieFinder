package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.BoxOffice
import com.choo.moviefinder.domain.model.BoxOfficeMovie
import com.choo.moviefinder.domain.model.BoxOfficeWeekType
import com.choo.moviefinder.domain.model.Movie
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// 매칭 로직 자체는 MatchBoxOfficeWithTmdbUseCaseTest가 검증한다.
// 이 테스트는 주간 조회 결과/weekType 전달 등 위임 계약만 확인한다.
class GetWeeklyBoxOfficeWithTmdbMatchUseCaseTest {

    private lateinit var getWeeklyBoxOfficeUseCase: GetWeeklyBoxOfficeUseCase
    private lateinit var matchBoxOfficeWithTmdbUseCase: MatchBoxOfficeWithTmdbUseCase
    private lateinit var useCase: GetWeeklyBoxOfficeWithTmdbMatchUseCase

    private fun boxOffice(rank: Int = 1, movieName: String = "테스트 영화") =
        BoxOffice(rank, 0, false, "cd$rank", movieName, "2024-01-01", 1000L, 5000L, 10_000_000L, 100)

    private fun movie(id: Int, title: String) =
        Movie(id, title, "/p.jpg", "/b.jpg", "overview", "2024-01-01", 7.5, 100)

    @Before
    fun setUp() {
        getWeeklyBoxOfficeUseCase = mockk()
        matchBoxOfficeWithTmdbUseCase = mockk()
        useCase = GetWeeklyBoxOfficeWithTmdbMatchUseCase(getWeeklyBoxOfficeUseCase, matchBoxOfficeWithTmdbUseCase)
    }

    @Test
    fun `invoke passes weekly box office result to match use case and returns its output`() = runTest {
        val boxOfficeList = listOf(boxOffice(movieName = "호프"))
        val matched = listOf(BoxOfficeMovie(boxOfficeList[0], movie(1, "호프")))
        coEvery { getWeeklyBoxOfficeUseCase(weekType = BoxOfficeWeekType.WEEK) } returns boxOfficeList
        coEvery { matchBoxOfficeWithTmdbUseCase(boxOfficeList) } returns matched

        val result = useCase()

        assertEquals(matched, result)
        coVerify(exactly = 1) { matchBoxOfficeWithTmdbUseCase(boxOfficeList) }
    }

    @Test
    fun `invoke defaults to WEEK week type when not specified`() = runTest {
        coEvery { getWeeklyBoxOfficeUseCase(weekType = BoxOfficeWeekType.WEEK) } returns emptyList()
        coEvery { matchBoxOfficeWithTmdbUseCase(emptyList()) } returns emptyList()

        useCase()

        coVerify(exactly = 1) { getWeeklyBoxOfficeUseCase(weekType = BoxOfficeWeekType.WEEK) }
        coVerify(exactly = 0) { getWeeklyBoxOfficeUseCase(weekType = BoxOfficeWeekType.WEEKEND) }
        coVerify(exactly = 0) { getWeeklyBoxOfficeUseCase(weekType = BoxOfficeWeekType.WEEKDAY) }
    }

    @Test
    fun `invoke forwards explicit weekType without targetDate`() = runTest {
        coEvery { getWeeklyBoxOfficeUseCase(weekType = BoxOfficeWeekType.WEEKEND) } returns emptyList()
        coEvery { matchBoxOfficeWithTmdbUseCase(emptyList()) } returns emptyList()

        useCase(weekType = BoxOfficeWeekType.WEEKEND)

        coVerify(exactly = 1) { getWeeklyBoxOfficeUseCase(weekType = BoxOfficeWeekType.WEEKEND) }
    }

    // 명시한 targetDate가 기본값(마지막 완료 주)으로 대체되지 않고 그대로 전달되는지 확인한다.
    // 기본값은 실행 날짜에 따라 달라지므로, 캡처한 실제 인자를 비교해 날짜 의존성을 없앤다.
    @Test
    fun `invoke forwards explicit targetDate and weekType together`() = runTest {
        val capturedDates = mutableListOf<String>()
        coEvery {
            getWeeklyBoxOfficeUseCase(capture(capturedDates), BoxOfficeWeekType.WEEKDAY)
        } returns emptyList()
        coEvery { matchBoxOfficeWithTmdbUseCase(emptyList()) } returns emptyList()

        useCase("20240101", BoxOfficeWeekType.WEEKDAY)

        assertEquals("20240101", capturedDates.single())
        coVerify(exactly = 1) { getWeeklyBoxOfficeUseCase("20240101", BoxOfficeWeekType.WEEKDAY) }
    }

    @Test
    fun `invoke delegates empty box office list and returns empty result`() = runTest {
        coEvery { getWeeklyBoxOfficeUseCase(weekType = BoxOfficeWeekType.WEEK) } returns emptyList()
        coEvery { matchBoxOfficeWithTmdbUseCase(emptyList()) } returns emptyList()

        val result = useCase()

        assertTrue(result.isEmpty())
        coVerify(exactly = 1) { matchBoxOfficeWithTmdbUseCase(emptyList()) }
    }

    @Test
    fun `invoke propagates exception from weekly box office use case without calling matcher`() = runTest {
        coEvery { getWeeklyBoxOfficeUseCase(weekType = BoxOfficeWeekType.WEEK) } throws
            java.io.IOException("network down")

        var thrown: Throwable? = null
        try {
            useCase()
        } catch (e: java.io.IOException) {
            thrown = e
        }

        assertEquals("network down", thrown?.message)
        coVerify(exactly = 0) { matchBoxOfficeWithTmdbUseCase(any()) }
    }
}
