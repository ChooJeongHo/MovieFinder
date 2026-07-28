package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.core.util.koficDefaultWeeklyTargetDate
import com.choo.moviefinder.domain.model.BoxOffice
import com.choo.moviefinder.domain.model.BoxOfficeWeekType
import com.choo.moviefinder.domain.repository.BoxOfficeRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetWeeklyBoxOfficeUseCase @Inject constructor(
    private val repository: BoxOfficeRepository
) {
    // targetDate(yyyyMMdd) 미지정 시 KST 기준 마지막으로 완료된 주의 박스오피스를 조회한다
    suspend operator fun invoke(
        targetDate: String = koficDefaultWeeklyTargetDate(),
        weekType: BoxOfficeWeekType = BoxOfficeWeekType.WEEK
    ): List<BoxOffice> = repository.getWeeklyBoxOffice(targetDate, weekType)
}
