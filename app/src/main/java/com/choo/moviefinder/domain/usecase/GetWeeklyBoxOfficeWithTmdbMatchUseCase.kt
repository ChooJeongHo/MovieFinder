package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.BoxOfficeMovie
import com.choo.moviefinder.domain.model.BoxOfficeWeekType
import dagger.Reusable
import javax.inject.Inject

// KOFIC 주간 박스오피스 TOP 10을 조회하고 TMDB 매칭 결과와 함께 돌려준다.
// 일별 경로와 매칭 전략을 공유하기 위해 MatchBoxOfficeWithTmdbUseCase에 위임한다.
@Reusable
class GetWeeklyBoxOfficeWithTmdbMatchUseCase @Inject constructor(
    private val getWeeklyBoxOfficeUseCase: GetWeeklyBoxOfficeUseCase,
    private val matchBoxOfficeWithTmdbUseCase: MatchBoxOfficeWithTmdbUseCase
) {
    suspend operator fun invoke(
        targetDate: String? = null,
        weekType: BoxOfficeWeekType = BoxOfficeWeekType.WEEK
    ): List<BoxOfficeMovie> {
        val boxOfficeList = targetDate
            ?.let { getWeeklyBoxOfficeUseCase(it, weekType) }
            ?: getWeeklyBoxOfficeUseCase(weekType = weekType)
        return matchBoxOfficeWithTmdbUseCase(boxOfficeList)
    }
}
