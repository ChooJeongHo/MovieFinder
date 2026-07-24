package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.core.util.koficDefaultTargetDate
import com.choo.moviefinder.domain.model.BoxOffice
import com.choo.moviefinder.domain.repository.BoxOfficeRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetDailyBoxOfficeUseCase @Inject constructor(
    private val repository: BoxOfficeRepository
) {
    // targetDate(yyyyMMdd) 미지정 시 KST 기준 전일 박스오피스를 조회한다
    suspend operator fun invoke(targetDate: String = koficDefaultTargetDate()): List<BoxOffice> =
        repository.getDailyBoxOffice(targetDate)
}
