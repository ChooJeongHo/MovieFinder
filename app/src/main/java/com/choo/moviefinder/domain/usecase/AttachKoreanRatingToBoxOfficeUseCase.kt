package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.BoxOfficeMovie
import dagger.Reusable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

// 박스오피스 목록 각 항목에 KMRB(영상물등회위원회) 관람등급을 부착한다.
// MatchBoxOfficeWithTmdbUseCase와 동일한 병렬 조회 패턴을 따른다.
// 조회 키는 반드시 boxOffice.movieName(KOFIC 원본 한국어 제목)이어야 한다 — matchedMovie?.title을 쓰면
// TMDB 매칭 실패(082일차 known issue) 항목의 등급이 영영 붙지 않게 된다.
// GetKoreanRatingUseCase가 이미 suspendRunCatching으로 실패를 흡수하므로 여기서 추가 try/catch는
// CancellationException 재전파 경로를 깨뜨릴 위험만 있어 넣지 않는다.
@Reusable
class AttachKoreanRatingToBoxOfficeUseCase @Inject constructor(
    private val getKoreanRatingUseCase: GetKoreanRatingUseCase
) {
    suspend operator fun invoke(movies: List<BoxOfficeMovie>): List<BoxOfficeMovie> = coroutineScope {
        if (movies.isEmpty()) return@coroutineScope emptyList()

        movies
            .map { item -> async { item.copy(koreanRating = getKoreanRatingUseCase(item.boxOffice.movieName)) } }
            .map { it.await() }
    }
}
