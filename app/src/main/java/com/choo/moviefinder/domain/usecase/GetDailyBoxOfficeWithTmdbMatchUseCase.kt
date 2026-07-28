package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.core.util.suspendRunCatching
import com.choo.moviefinder.domain.model.BoxOfficeMovie
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.MovieQueryRepository
import dagger.Reusable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import javax.inject.Inject

// KOFIC 일별 박스오피스 TOP 10을 조회하고, 각 영화를 TMDB 검색 결과와 제목 기준으로 매칭한다.
// 두 API는 서로 다른 식별자 체계를 쓰기 때문에(KOFIC movieCd vs TMDB id) ID로 직접 연결할 수 없고,
// movieNm(영화명) 문자열 매칭에 의존한다 — 매칭 정확도와 관련된 트레이드오프는 CLAUDE.md/응답 참고.
// 로컬 캐시(현재 상영작/인기 영화)를 먼저 한 번만 조회해 우선 매칭하고, 캐시에 없는 항목만 TMDB 네트워크
// 검색으로 폴백한다 — 순위 범위(TOP N)가 늘어도 네트워크 호출이 선형으로 증가하는 N+1 문제를 피하기 위함.
@Reusable
class GetDailyBoxOfficeWithTmdbMatchUseCase @Inject constructor(
    private val getDailyBoxOfficeUseCase: GetDailyBoxOfficeUseCase,
    private val movieQueryRepository: MovieQueryRepository
) {
    suspend operator fun invoke(targetDate: String? = null): List<BoxOfficeMovie> = coroutineScope {
        val boxOfficeList = targetDate?.let { getDailyBoxOfficeUseCase(it) } ?: getDailyBoxOfficeUseCase()
        if (boxOfficeList.isEmpty()) return@coroutineScope emptyList()

        val cachedMovies = suspendRunCatching { movieQueryRepository.getCachedMoviesSnapshot() }
            .onFailure { Timber.w(it, "박스오피스 매칭용 로컬 캐시 조회 실패") }
            .getOrElse { emptyList() }

        boxOfficeList
            .map { boxOffice -> async { BoxOfficeMovie(boxOffice, findMatch(boxOffice.movieName, cachedMovies)) } }
            .map { it.await() }
    }

    // 로컬 캐시에서 먼저 제목이 일치하는 영화를 찾고, 캐시 미스일 때만 TMDB 네트워크 검색으로 폴백한다.
    private suspend fun findMatch(movieName: String, cachedMovies: List<Movie>): Movie? =
        cachedMovies.firstOrNull { titleMatches(it.title, movieName) } ?: findMatchFromNetwork(movieName)

    // TMDB에서 제목으로 검색해 정규화된 제목이 일치하는 첫 결과를 매칭 영화로 채택한다.
    // 검색 자체가 실패해도(네트워크 오류 등) 전체 목록을 무너뜨리지 않고 해당 항목만 매칭 실패로 처리한다.
    private suspend fun findMatchFromNetwork(movieName: String): Movie? =
        suspendRunCatching { movieQueryRepository.searchMoviesOnce(movieName) }
            .onFailure { Timber.w(it, "박스오피스 '%s' TMDB 검색 실패", movieName) }
            .getOrElse { emptyList() }
            .firstOrNull { candidate -> titleMatches(candidate.title, movieName) }

    private fun titleMatches(candidateTitle: String, movieName: String): Boolean =
        normalizeTitle(candidateTitle) == normalizeTitle(movieName)

    // 공백/구두점/대소문자 차이로 인한 오탐지를 줄이기 위한 제목 정규화
    private fun normalizeTitle(title: String): String =
        title.lowercase().filterNot { it.isWhitespace() || it in IGNORED_PUNCTUATION }

    companion object {
        private val IGNORED_PUNCTUATION = setOf(':', '-', '!', '?', '.', ',', '·', '\'', '"', '(', ')')
    }
}
