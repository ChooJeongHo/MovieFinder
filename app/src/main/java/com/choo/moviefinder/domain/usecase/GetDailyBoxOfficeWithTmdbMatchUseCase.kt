package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.BoxOfficeMovie
import dagger.Reusable
import javax.inject.Inject

// KOFIC 일별 박스오피스 TOP 10을 조회하고 TMDB 매칭 결과와 함께 돌려준다.
// 매칭/캐시 전략 자체는 일별·주간이 동일하므로 MatchBoxOfficeWithTmdbUseCase에 위임한다.
@Reusable
class GetDailyBoxOfficeWithTmdbMatchUseCase @Inject constructor(
    private val getDailyBoxOfficeUseCase: GetDailyBoxOfficeUseCase,
    private val matchBoxOfficeWithTmdbUseCase: MatchBoxOfficeWithTmdbUseCase
) {
    suspend operator fun invoke(targetDate: String? = null): List<BoxOfficeMovie> {
        val boxOfficeList = targetDate?.let { getDailyBoxOfficeUseCase(it) } ?: getDailyBoxOfficeUseCase()
        return matchBoxOfficeWithTmdbUseCase(boxOfficeList)
    }
}
