package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.BoxOffice
import com.choo.moviefinder.domain.model.BoxOfficeWeekType

interface BoxOfficeRepository {

    // targetDate(yyyyMMdd)의 일별 박스오피스 순위를 조회한다
    suspend fun getDailyBoxOffice(targetDate: String): List<BoxOffice>

    // targetDate(yyyyMMdd)가 속한 주의 박스오피스 순위를 weekType 구분으로 조회한다
    suspend fun getWeeklyBoxOffice(
        targetDate: String,
        weekType: BoxOfficeWeekType = BoxOfficeWeekType.WEEK
    ): List<BoxOffice>
}
