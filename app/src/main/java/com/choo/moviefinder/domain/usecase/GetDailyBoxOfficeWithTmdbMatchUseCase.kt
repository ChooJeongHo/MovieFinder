package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.core.util.suspendRunCatching
import com.choo.moviefinder.domain.model.BoxOfficeMovie
import com.choo.moviefinder.domain.repository.MovieQueryRepository
import dagger.Reusable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import javax.inject.Inject

// KOFIC 일별 박스오피스 TOP 10을 조회하고, 각 영화를 TMDB 검색 결과와 제목 기준으로 매칭한다.
// 두 API는 서로 다른 식별자 체계를 쓰기 때문에(KOFIC movieCd vs TMDB id) ID로 직접 연결할 수 없고,
// movieNm(영화명) 문자열 매칭에 의존한다 — 매칭 정확도와 관련된 트레이드오프는 CLAUDE.md/응답 참고.
@Reusable
class GetDailyBoxOfficeWithTmdbMatchUseCase @Inject constructor(
    private val getDailyBoxOfficeUseCase: GetDailyBoxOfficeUseCase,
    private val movieQueryRepository: MovieQueryRepository
) {
    suspend operator fun invoke(targetDate: String? = null): List<BoxOfficeMovie> = coroutineScope {
        val boxOfficeList = targetDate?.let { getDailyBoxOfficeUseCase(it) } ?: getDailyBoxOfficeUseCase()

        boxOfficeList
            .map { boxOffice -> async { BoxOfficeMovie(boxOffice, findMatch(boxOffice.movieName)) } }
            .map { it.await() }
    }

    // TMDB에서 제목으로 검색해 정규화된 제목이 일치하는 첫 결과를 매칭 영화로 채택한다.
    // 검색 자체가 실패해도(네트워크 오류 등) 전체 목록을 무너뜨리지 않고 해당 항목만 매칭 실패로 처리한다.
    private suspend fun findMatch(movieName: String) =
        suspendRunCatching { movieQueryRepository.searchMoviesOnce(movieName) }
            .onFailure { Timber.w(it, "박스오피스 '%s' TMDB 검색 실패", movieName) }
            .getOrElse { emptyList() }
            .firstOrNull { candidate -> normalizeTitle(candidate.title) == normalizeTitle(movieName) }

    // 공백/구두점/대소문자 차이로 인한 오탐지를 줄이기 위한 제목 정규화
    private fun normalizeTitle(title: String): String =
        title.lowercase().filterNot { it.isWhitespace() || it in IGNORED_PUNCTUATION }

    companion object {
        private val IGNORED_PUNCTUATION = setOf(':', '-', '!', '?', '.', ',', '·', '\'', '"', '(', ')')
    }
}
