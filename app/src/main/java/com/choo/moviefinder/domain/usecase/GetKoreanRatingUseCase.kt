package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.core.util.suspendRunCatching
import com.choo.moviefinder.domain.model.KoreanRating
import com.choo.moviefinder.domain.repository.KoreanRatingRepository
import dagger.Reusable
import timber.log.Timber
import javax.inject.Inject

// KMRB(영상물등급위원회) 등급분류정보를 movieTitle로 조회한다. 캐시/매칭 로직은
// KoreanRatingRepositoryImpl로 이동했고, 이 UseCase는 실패를 흡수하는 얇은 래퍼로 남는다.
@Reusable
class GetKoreanRatingUseCase @Inject constructor(
    private val repository: KoreanRatingRepository
) {
    suspend operator fun invoke(movieTitle: String): KoreanRating? {
        if (movieTitle.isBlank()) return null
        return suspendRunCatching { repository.getRatingForMovie(movieTitle) }
            .onFailure { Timber.w(it, "영등위 등급 조회 실패: %s", movieTitle) }
            .getOrNull()
    }
}
