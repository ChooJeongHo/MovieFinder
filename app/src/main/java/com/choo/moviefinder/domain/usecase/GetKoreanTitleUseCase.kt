package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.core.util.suspendRunCatching
import com.choo.moviefinder.domain.repository.MovieDetailRepository
import dagger.Reusable
import timber.log.Timber
import javax.inject.Inject

// movieId로 한국어(ko-KR) 제목을 조회한다 (KMRB 등급 매칭용). 화면 표시 title이 앱 언어를 따르더라도
// 이 값은 항상 한국어를 유지해야 KOFIC/KMRB와 매칭 가능하다. Repository 레벨에서 movieId 기준으로
// 영구 캐시되어, 등급 필터가 켜질 때마다 반복 조회되는 N+1을 피한다.
@Reusable
class GetKoreanTitleUseCase @Inject constructor(
    private val movieDetailRepository: MovieDetailRepository
) {
    suspend operator fun invoke(movieId: Int): String? =
        suspendRunCatching { movieDetailRepository.getKoreanTitle(movieId) }
            .onFailure { Timber.w(it, "한국어 제목 조회 실패: movieId=%d", movieId) }
            .getOrNull()
}
