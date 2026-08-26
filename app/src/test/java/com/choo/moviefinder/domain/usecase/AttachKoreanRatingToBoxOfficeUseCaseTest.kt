package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.BoxOffice
import com.choo.moviefinder.domain.model.BoxOfficeMovie
import com.choo.moviefinder.domain.model.KoreanRating
import com.choo.moviefinder.domain.model.Movie
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AttachKoreanRatingToBoxOfficeUseCaseTest {

    private lateinit var getKoreanRatingUseCase: GetKoreanRatingUseCase
    private lateinit var useCase: AttachKoreanRatingToBoxOfficeUseCase

    private fun boxOffice(rank: Int = 1, movieName: String = "테스트 영화") =
        BoxOffice(rank, 0, false, "cd$rank", movieName, "2024-01-01", 1000L, 5000L, 10_000_000L, 100)

    private fun movie(id: Int, title: String) =
        Movie(id, title, "/p.jpg", "/b.jpg", "overview", "2024-01-01", 7.5, 100)

    private fun rating(gradeName: String = "15세이상관람가", title: String = "테스트 영화") =
        KoreanRating(gradeName, title, "제작사", "감독", "2024", "사유")

    @Before
    fun setUp() {
        getKoreanRatingUseCase = mockk()
        useCase = AttachKoreanRatingToBoxOfficeUseCase(getKoreanRatingUseCase)
    }

    // ① 빈 리스트는 조기 반환돼야 하고, GetKoreanRatingUseCase를 전혀 호출하면 안 된다
    @Test
    fun `invoke returns empty list without calling GetKoreanRatingUseCase when input is empty`() = runTest {
        val result = useCase(emptyList())

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { getKoreanRatingUseCase(any()) }
    }

    // ② 정상 부착: koreanRating이 세팅되고 boxOffice/matchedMovie는 그대로 유지된다
    @Test
    fun `invoke attaches koreanRating while keeping boxOffice and matchedMovie unchanged`() = runTest {
        val bo = boxOffice(movieName = "테스트 영화")
        val matched = movie(1, "테스트 영화")
        val item = BoxOfficeMovie(bo, matched)
        coEvery { getKoreanRatingUseCase("테스트 영화") } returns rating()

        val result = useCase(listOf(item))

        assertEquals(1, result.size)
        assertEquals(bo, result[0].boxOffice)
        assertEquals(matched, result[0].matchedMovie)
        assertEquals(rating(), result[0].koreanRating)
    }

    // ③ 등급 조회가 null을 반환해도 항목 자체가 누락되지 않고 koreanRating만 null로 유지된다 (필터 아님)
    @Test
    fun `invoke keeps item with null koreanRating when rating lookup returns null`() = runTest {
        val item = BoxOfficeMovie(boxOffice(movieName = "등급없음"), matchedMovie = null)
        coEvery { getKoreanRatingUseCase("등급없음") } returns null

        val result = useCase(listOf(item))

        assertEquals(1, result.size)
        assertNull(result[0].koreanRating)
    }

    // ④ 회귀 방지 핵심: 조회 키가 반드시 boxOffice.movieName이어야 한다 (matchedMovie?.title 사용 금지)
    @Test
    fun `invoke queries rating using boxOffice movieName not matchedMovie title`() = runTest {
        val bo = boxOffice(movieName = "코픽 원본 제목")
        val matched = movie(1, "TMDB 매칭 제목")
        coEvery { getKoreanRatingUseCase("코픽 원본 제목") } returns rating(title = "코픽 원본 제목")

        useCase(listOf(BoxOfficeMovie(bo, matched)))

        coVerify(exactly = 1) { getKoreanRatingUseCase("코픽 원본 제목") }
        coVerify(exactly = 0) { getKoreanRatingUseCase("TMDB 매칭 제목") }
    }

    // ⑤ TMDB 매칭 실패(matchedMovie == null) 항목도 등급이 정상적으로 붙어야 한다
    @Test
    fun `invoke attaches rating even when matchedMovie is null`() = runTest {
        val item = BoxOfficeMovie(boxOffice(movieName = "매칭 실패 영화"), matchedMovie = null)
        coEvery { getKoreanRatingUseCase("매칭 실패 영화") } returns rating(title = "매칭 실패 영화")

        val result = useCase(listOf(item))

        assertNull(result[0].matchedMovie)
        assertEquals("매칭 실패 영화", result[0].koreanRating?.title)
    }

    // ⑥ 병렬성: 5개 항목이 순차(500ms)가 아니라 병렬(~100ms)로 끝나야 한다
    @Test
    fun `invoke fetches ratings in parallel not sequentially`() = runTest {
        val input = (1..5).map { BoxOfficeMovie(boxOffice(rank = it, movieName = "영화$it"), matchedMovie = null) }
        input.forEach { item ->
            coEvery { getKoreanRatingUseCase(item.boxOffice.movieName) } coAnswers {
                delay(100)
                rating(title = item.boxOffice.movieName)
            }
        }

        useCase(input)

        // 순차 실행이었다면 500ms가 걸렸어야 하지만, 병렬 실행이므로 100ms 근처(가상 시간)에서 끝난다.
        assertTrue("elapsed=$currentTime", currentTime < 200)
    }
}
