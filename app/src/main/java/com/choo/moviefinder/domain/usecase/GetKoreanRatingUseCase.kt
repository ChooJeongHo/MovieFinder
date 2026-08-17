package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.core.util.suspendRunCatching
import com.choo.moviefinder.domain.model.KoreanRating
import com.choo.moviefinder.domain.repository.KoreanRatingRepository
import dagger.Reusable
import timber.log.Timber
import javax.inject.Inject

// KMRB(영상물등급위원회) 등급분류정보를 movieTitle로 조회해 제목이 일치하는 첫 결과를 반환한다.
// MatchBoxOfficeWithTmdbUseCase와 동일한 제목 정규화/매칭 로직을 사용한다(공통 유틸 추출은 이번 범위 밖).
@Reusable
class GetKoreanRatingUseCase @Inject constructor(
    private val repository: KoreanRatingRepository
) {
    suspend operator fun invoke(movieTitle: String): KoreanRating? {
        if (movieTitle.isBlank()) return null
        return suspendRunCatching { repository.searchRatings(movieTitle) }
            .onFailure { Timber.w(it, "영등위 등급 조회 실패: %s", movieTitle) }
            .getOrElse { emptyList() }
            .firstOrNull { candidate -> titleMatches(candidate.title, movieTitle) }
    }

    private fun titleMatches(candidateTitle: String, movieTitle: String): Boolean =
        normalizeTitle(candidateTitle) == normalizeTitle(movieTitle)

    private fun normalizeTitle(title: String): String =
        title.lowercase().filterNot { it.isWhitespace() || it in IGNORED_PUNCTUATION }

    companion object {
        private val IGNORED_PUNCTUATION = setOf(':', '-', '!', '?', '.', ',', '·', '\'', '"', '(', ')')
    }
}
