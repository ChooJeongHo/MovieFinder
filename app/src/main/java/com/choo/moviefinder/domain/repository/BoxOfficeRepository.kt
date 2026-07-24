package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.BoxOffice

interface BoxOfficeRepository {

    // targetDate(yyyyMMdd)의 일별 박스오피스 순위를 조회한다
    suspend fun getDailyBoxOffice(targetDate: String): List<BoxOffice>
}
