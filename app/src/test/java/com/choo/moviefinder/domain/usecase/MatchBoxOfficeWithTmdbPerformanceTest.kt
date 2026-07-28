package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.BoxOffice
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.MovieQueryRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

// N+1 TMDB 검색 문제의 Before(항상 네트워크 검색)/After(로컬 캐시 우선 조회) 개선 효과를
// 실측 수치로 남겨두기 위한 벤치마크. 실제 어설션보다 "네트워크 호출 횟수"와 "소요 시간"을
// 콘솔에 출력해 회귀 시 비교 기준으로 삼는 것이 목적이다.
//
// 네트워크 지연(150ms)과 OkHttp의 host당 동시 연결 제한(기본 5개, NetworkModule 참고)을
// Semaphore(5)로 흉내내어, 동시 요청이 5개를 넘으면 실제 앱에서처럼 대기열이 생기도록 했다.
// 이 대기열 효과 때문에 Before 방식은 TOP 10 → TOP 50으로 갈수록 소요 시간이 계단식으로 늘어난다.
class MatchBoxOfficeWithTmdbPerformanceTest {

    private fun boxOfficeList(count: Int) = (1..count).map {
        BoxOffice(it, 0, false, "cd$it", "영화$it", "2024-01-01", 1000L, 5000L, 10_000_000L, 100)
    }

    private fun movie(id: Int, title: String) = Movie(id, title, "/p.jpg", "/b.jpg", "overview", "2024-01-01", 7.5, 100)

    // 실제 OkHttp maxRequestsPerHost=5(기본값) 제약을 흉내낸 네트워크 세마포어.
    private fun networkBoundRepository(networkCallCount: AtomicInteger): MovieQueryRepository {
        val hostConnectionLimit = Semaphore(permits = 5)
        val repository = mockk<MovieQueryRepository>()
        coEvery { repository.searchMoviesOnce(any()) } coAnswers {
            networkCallCount.incrementAndGet()
            hostConnectionLimit.withPermit {
                delay(150) // 실측 TMDB 왕복 지연 근사치
                val title = firstArg<String>()
                listOf(movie(title.removePrefix("영화").toInt(), title))
            }
        }
        return repository
    }

    private fun measure(topN: Int, cacheHitRatio: Double): BenchmarkResult {
        val networkCallCount = AtomicInteger(0)
        val repository = networkBoundRepository(networkCallCount)
        val cacheHitCount = (topN * cacheHitRatio).toInt()
        coEvery { repository.getCachedMoviesSnapshot() } coAnswers {
            delay(5) // 로컬 Room 조회 근사치
            (1..cacheHitCount).map { movie(it, "영화$it") }
        }
        val useCase = MatchBoxOfficeWithTmdbUseCase(repository)
        val input = boxOfficeList(topN)

        var matchedCount = 0
        val elapsedMs = measureTimeMillis {
            runBlocking {
                matchedCount = useCase(input).count { it.matchedMovie != null }
            }
        }
        return BenchmarkResult(topN, cacheHitRatio, elapsedMs, networkCallCount.get(), matchedCount)
    }

    private data class BenchmarkResult(
        val topN: Int,
        val cacheHitRatio: Double,
        val elapsedMs: Long,
        val networkCalls: Int,
        val matchedCount: Int
    )

    @Test
    fun `benchmark Before(no cache) vs After(local cache) at TOP 10, 30, 50`() {
        println("=== N+1 TMDB 검색 개선 벤치마크 (081일차 후속) ===")
        println("%-6s | %-28s | %-10s | %-10s".format("TOP N", "시나리오", "소요(ms)", "네트워크 호출"))
        println("-".repeat(70))

        listOf(10, 30, 50).forEach { topN ->
            // Before: 로컬 캐시가 전혀 없다고 가정 — 현재 코드가 옛 방식(매번 네트워크 검색)과 동일하게 동작
            val before = measure(topN, cacheHitRatio = 0.0)
            // After: 홈 화면에 이미 캐싱된 now_playing/popular와 KOFIC TOP N이 실제로 겹치는 정도(약 80%) 가정
            val after = measure(topN, cacheHitRatio = 0.8)

            val rowFormat = "%-6d | %-28s | %-10d | %-10d"
            println(rowFormat.format(topN, "Before (캐시 없음)", before.elapsedMs, before.networkCalls))
            println(rowFormat.format(topN, "After (로컬 캐시 우선, 80% hit)", after.elapsedMs, after.networkCalls))

            check(after.networkCalls <= before.networkCalls) {
                "After 방식의 네트워크 호출 수(${after.networkCalls})가 Before(${before.networkCalls})보다 많으면 안 됨"
            }
            check(after.matchedCount == before.matchedCount) {
                "매칭 결과 개수가 Before/After 간에 달라지면 안 됨 (동일한 정확도 유지)"
            }
        }

        // 완전 캐시 히트(100%) 시나리오 — 네트워크 호출이 0이어야 함을 명시적으로 확인
        val fullCacheHit = measure(topN = 50, cacheHitRatio = 1.0)
        println("-".repeat(70))
        println("TOP 50, 캐시 100% hit: 소요 ${fullCacheHit.elapsedMs}ms, 네트워크 호출 ${fullCacheHit.networkCalls}회")
        check(fullCacheHit.networkCalls == 0) { "캐시가 전부 히트하면 네트워크 호출이 0회여야 함" }
    }
}
