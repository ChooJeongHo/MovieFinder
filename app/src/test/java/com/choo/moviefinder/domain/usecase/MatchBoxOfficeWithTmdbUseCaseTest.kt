package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.BoxOffice
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.MovieQueryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// 일별/주간 박스오피스가 공유하는 TMDB 매칭 로직 검증
// (GetDailyBoxOfficeWithTmdbMatchUseCaseTest에 있던 매칭 케이스를 이관해 온 것)
class MatchBoxOfficeWithTmdbUseCaseTest {

    private lateinit var movieQueryRepository: MovieQueryRepository
    private lateinit var useCase: MatchBoxOfficeWithTmdbUseCase

    private fun boxOffice(rank: Int = 1, movieName: String = "테스트 영화") =
        BoxOffice(rank, 0, false, "cd$rank", movieName, "2024-01-01", 1000L, 5000L, 10_000_000L, 100)

    private fun movie(id: Int, title: String) =
        Movie(id, title, "/p.jpg", "/b.jpg", "overview", "2024-01-01", 7.5, 100)

    @Before
    fun setUp() {
        movieQueryRepository = mockk()
        // 기본값: 로컬 캐시가 비어있는 상태(캐시 미스) — 캐시 히트를 검증하는 테스트만 개별적으로 재정의한다
        coEvery { movieQueryRepository.getCachedMoviesSnapshot() } returns emptyList()
        useCase = MatchBoxOfficeWithTmdbUseCase(movieQueryRepository)
    }

    @Test
    fun `invoke matches TMDB movie with exactly equal title`() = runTest {
        coEvery { movieQueryRepository.searchMoviesOnce("테스트 영화") } returns listOf(movie(1, "테스트 영화"))

        val result = useCase(listOf(boxOffice(movieName = "테스트 영화")))

        assertEquals(1, result.size)
        assertNotNull(result[0].matchedMovie)
        assertEquals(1, result[0].matchedMovie?.id)
    }

    @Test
    fun `invoke matches titles differing only by whitespace and punctuation`() = runTest {
        coEvery { movieQueryRepository.searchMoviesOnce("탑건: 매버릭") } returns listOf(movie(2, "탑건:매버릭"))

        val result = useCase(listOf(boxOffice(movieName = "탑건: 매버릭")))

        assertNotNull(result[0].matchedMovie)
        assertEquals(2, result[0].matchedMovie?.id)
    }

    @Test
    fun `invoke leaves matchedMovie null when no TMDB result matches title`() = runTest {
        coEvery { movieQueryRepository.searchMoviesOnce("완전히 다른 제목") } returns
            listOf(movie(3, "전혀 상관없는 영화"))

        val result = useCase(listOf(boxOffice(movieName = "완전히 다른 제목")))

        assertNull(result[0].matchedMovie)
    }

    @Test
    fun `invoke leaves matchedMovie null when TMDB search returns no results`() = runTest {
        coEvery { movieQueryRepository.searchMoviesOnce("테스트 영화") } returns emptyList()

        val result = useCase(listOf(boxOffice()))

        assertNull(result[0].matchedMovie)
    }

    @Test
    fun `invoke leaves matchedMovie null and does not throw when TMDB search fails`() = runTest {
        coEvery { movieQueryRepository.searchMoviesOnce("테스트 영화") } throws RuntimeException("network error")

        val result = useCase(listOf(boxOffice()))

        assertEquals(1, result.size)
        assertNull(result[0].matchedMovie)
    }

    @Test
    fun `invoke processes remaining items when one search fails`() = runTest {
        coEvery { movieQueryRepository.searchMoviesOnce("실패 영화") } throws RuntimeException("network error")
        coEvery { movieQueryRepository.searchMoviesOnce("성공 영화") } returns listOf(movie(4, "성공 영화"))

        val result = useCase(
            listOf(
                boxOffice(rank = 1, movieName = "실패 영화"),
                boxOffice(rank = 2, movieName = "성공 영화")
            )
        )

        assertEquals(2, result.size)
        assertNull(result.first { it.boxOffice.rank == 1 }.matchedMovie)
        assertEquals(4, result.first { it.boxOffice.rank == 2 }.matchedMovie?.id)
    }

    // 빈 입력은 로컬 캐시 조회(Room I/O)조차 하지 않고 곧바로 단축 반환돼야 한다
    @Test
    fun `invoke returns empty list without touching repository when input is empty`() = runTest {
        val result = useCase(emptyList())

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { movieQueryRepository.getCachedMoviesSnapshot() }
        coVerify(exactly = 0) { movieQueryRepository.searchMoviesOnce(any()) }
    }

    @Test
    fun `invoke uses first matching result when TMDB returns multiple candidates`() = runTest {
        coEvery { movieQueryRepository.searchMoviesOnce("동명 영화") } returns
            listOf(movie(5, "동명 영화"), movie(6, "동명 영화"))

        val result = useCase(listOf(boxOffice(movieName = "동명 영화")))

        assertEquals(5, result[0].matchedMovie?.id)
    }

    // async 병렬 매칭 후에도 결과가 입력 순위 순서를 그대로 유지해야 한다
    @Test
    fun `invoke preserves input order after parallel matching`() = runTest {
        val input = (1..5).map { boxOffice(rank = it, movieName = "영화$it") }
        input.forEach { bo ->
            coEvery { movieQueryRepository.searchMoviesOnce(bo.movieName) } returns
                listOf(movie(bo.rank * 100, bo.movieName))
        }

        val result = useCase(input)

        assertEquals(listOf(1, 2, 3, 4, 5), result.map { it.boxOffice.rank })
        assertEquals(listOf("영화1", "영화2", "영화3", "영화4", "영화5"), result.map { it.boxOffice.movieName })
        assertEquals(listOf(100, 200, 300, 400, 500), result.map { it.matchedMovie?.id })
    }

    // --- 로컬 캐시 우선 조회 (cache hit / miss) ---

    @Test
    fun `invoke matches from local cache and never calls TMDB network search on cache hit`() = runTest {
        coEvery { movieQueryRepository.getCachedMoviesSnapshot() } returns listOf(movie(7, "테스트 영화"))

        val result = useCase(listOf(boxOffice(movieName = "테스트 영화")))

        assertEquals(7, result[0].matchedMovie?.id)
        coVerify(exactly = 0) { movieQueryRepository.searchMoviesOnce(any()) }
    }

    @Test
    fun `invoke matches from local cache with whitespace and punctuation normalization`() = runTest {
        coEvery { movieQueryRepository.getCachedMoviesSnapshot() } returns listOf(movie(8, "탑건:매버릭"))

        val result = useCase(listOf(boxOffice(movieName = "탑건: 매버릭")))

        assertEquals(8, result[0].matchedMovie?.id)
        coVerify(exactly = 0) { movieQueryRepository.searchMoviesOnce(any()) }
    }

    @Test
    fun `invoke falls back to network search when local cache has no matching title`() = runTest {
        coEvery { movieQueryRepository.getCachedMoviesSnapshot() } returns listOf(movie(9, "전혀 다른 캐시 영화"))
        coEvery { movieQueryRepository.searchMoviesOnce("신작 영화") } returns listOf(movie(10, "신작 영화"))

        val result = useCase(listOf(boxOffice(movieName = "신작 영화")))

        assertEquals(10, result[0].matchedMovie?.id)
        coVerify(exactly = 1) { movieQueryRepository.searchMoviesOnce("신작 영화") }
    }

    @Test
    fun `invoke queries local cache snapshot only once regardless of box office list size`() = runTest {
        coEvery { movieQueryRepository.getCachedMoviesSnapshot() } returns (1..10).map { movie(it, "영화$it") }

        val result = useCase((1..10).map { boxOffice(rank = it, movieName = "영화$it") })

        assertEquals(10, result.size)
        assertTrue(result.all { it.matchedMovie != null })
        coVerify(exactly = 1) { movieQueryRepository.getCachedMoviesSnapshot() }
        coVerify(exactly = 0) { movieQueryRepository.searchMoviesOnce(any()) }
    }

    @Test
    fun `invoke uses cache for some items and network fallback for others in the same run`() = runTest {
        coEvery { movieQueryRepository.getCachedMoviesSnapshot() } returns listOf(movie(11, "캐시에 있는 영화"))
        coEvery { movieQueryRepository.searchMoviesOnce("캐시에 없는 영화") } returns
            listOf(movie(12, "캐시에 없는 영화"))

        val result = useCase(
            listOf(
                boxOffice(rank = 1, movieName = "캐시에 있는 영화"),
                boxOffice(rank = 2, movieName = "캐시에 없는 영화")
            )
        )

        assertEquals(11, result.first { it.boxOffice.rank == 1 }.matchedMovie?.id)
        assertEquals(12, result.first { it.boxOffice.rank == 2 }.matchedMovie?.id)
        coVerify(exactly = 1) { movieQueryRepository.searchMoviesOnce("캐시에 없는 영화") }
        coVerify(exactly = 0) { movieQueryRepository.searchMoviesOnce("캐시에 있는 영화") }
    }

    @Test
    fun `invoke falls back to network when local cache snapshot fetch itself fails`() = runTest {
        coEvery { movieQueryRepository.getCachedMoviesSnapshot() } throws RuntimeException("db error")
        coEvery { movieQueryRepository.searchMoviesOnce("테스트 영화") } returns listOf(movie(13, "테스트 영화"))

        val result = useCase(listOf(boxOffice(movieName = "테스트 영화")))

        assertEquals(13, result[0].matchedMovie?.id)
    }
}
