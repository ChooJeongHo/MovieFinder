package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.BoxOffice
import com.choo.moviefinder.domain.model.BoxOfficeMovie
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
// 이 테스트는 "일별 조회 결과를 그대로 매칭 UseCase에 넘기는지"(위임 계약)만 확인한다.
class GetDailyBoxOfficeWithTmdbMatchUseCaseTest {

    private lateinit var getDailyBoxOfficeUseCase: GetDailyBoxOfficeUseCase
    private lateinit var matchBoxOfficeWithTmdbUseCase: MatchBoxOfficeWithTmdbUseCase
    private lateinit var useCase: GetDailyBoxOfficeWithTmdbMatchUseCase

    private fun boxOffice(rank: Int = 1, movieName: String = "테스트 영화") =
        BoxOffice(rank, 0, false, "cd$rank", movieName, "2024-01-01", 1000L, 5000L, 10_000_000L, 100)

    private fun movie(id: Int, title: String) =
        Movie(id, title, "/p.jpg", "/b.jpg", "overview", "2024-01-01", 7.5, 100)

    @Before
    fun setUp() {
        getDailyBoxOfficeUseCase = mockk()
        matchBoxOfficeWithTmdbUseCase = mockk()
        useCase = GetDailyBoxOfficeWithTmdbMatchUseCase(getDailyBoxOfficeUseCase, matchBoxOfficeWithTmdbUseCase)
    }

    @Test
    fun `invoke passes daily box office result to match use case and returns its output`() = runTest {
        val boxOfficeList = listOf(boxOffice(movieName = "테스트 영화"))
        val matched = listOf(BoxOfficeMovie(boxOfficeList[0], movie(1, "테스트 영화")))
        coEvery { getDailyBoxOfficeUseCase() } returns boxOfficeList
        coEvery { matchBoxOfficeWithTmdbUseCase(boxOfficeList) } returns matched

        val result = useCase()

        assertEquals(matched, result)
        coVerify(exactly = 1) { matchBoxOfficeWithTmdbUseCase(boxOfficeList) }
    }

    @Test
    fun `invoke passes explicit targetDate through to GetDailyBoxOfficeUseCase`() = runTest {
        coEvery { getDailyBoxOfficeUseCase("20240315") } returns emptyList()
        coEvery { matchBoxOfficeWithTmdbUseCase(emptyList()) } returns emptyList()

        useCase("20240315")

        coVerify(exactly = 1) { getDailyBoxOfficeUseCase("20240315") }
        coVerify(exactly = 0) { getDailyBoxOfficeUseCase() }
    }

    // 빈 목록도 매칭 UseCase에 그대로 위임한다 (단축 처리는 매칭 UseCase 책임)
    @Test
    fun `invoke delegates empty box office list and returns empty result`() = runTest {
        coEvery { getDailyBoxOfficeUseCase() } returns emptyList()
        coEvery { matchBoxOfficeWithTmdbUseCase(emptyList()) } returns emptyList()

        val result = useCase()

        assertTrue(result.isEmpty())
        coVerify(exactly = 1) { matchBoxOfficeWithTmdbUseCase(emptyList()) }
    }

    @Test
    fun `invoke propagates exception from daily box office use case without calling matcher`() = runTest {
        coEvery { getDailyBoxOfficeUseCase() } throws java.io.IOException("network down")

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
