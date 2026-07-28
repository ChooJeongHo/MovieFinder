package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.remote.api.KoficApiService
import com.choo.moviefinder.data.remote.dto.KoficFaultInfo
import com.choo.moviefinder.data.remote.dto.toDomain
import com.choo.moviefinder.data.remote.dto.KoficBoxOfficeResponse
import com.choo.moviefinder.data.remote.dto.KoficBoxOfficeResult
import com.choo.moviefinder.data.util.safeApiCall
import com.choo.moviefinder.domain.model.BoxOffice
import com.choo.moviefinder.domain.model.BoxOfficeWeekType
import com.choo.moviefinder.domain.model.DomainException
import com.choo.moviefinder.domain.repository.BoxOfficeRepository
import javax.inject.Inject

class BoxOfficeRepositoryImpl @Inject constructor(
    private val apiService: KoficApiService
) : BoxOfficeRepository {

    // KOFIC 일별 박스오피스 TOP 10을 조회한다
    override suspend fun getDailyBoxOffice(targetDate: String): List<BoxOffice> {
        requireValidTargetDate(targetDate)
        val response = safeApiCall { apiService.getDailyBoxOffice(targetDate) }
        return response.requireResult().dailyBoxOfficeList.map { it.toDomain() }
    }

    // KOFIC 주간(주간/주말/주중) 박스오피스 TOP 10을 조회한다
    override suspend fun getWeeklyBoxOffice(
        targetDate: String,
        weekType: BoxOfficeWeekType
    ): List<BoxOffice> {
        requireValidTargetDate(targetDate)
        val response = safeApiCall { apiService.getWeeklyBoxOffice(targetDate, weekType.toWeekGb()) }
        return response.requireResult().weeklyBoxOfficeList.map { it.toDomain() }
    }

    private fun requireValidTargetDate(targetDate: String) {
        require(TARGET_DATE_REGEX.matches(targetDate)) { "targetDate must be yyyyMMdd format" }
    }

    // boxOfficeResult 자체가 없을 때만 실패로 간주한다 — 조회 결과가 없는 날짜는 빈 배열로 정상 응답된다.
    private fun KoficBoxOfficeResponse.requireResult(): KoficBoxOfficeResult =
        boxOfficeResult ?: throw faultInfo.toDomainException()

    // KOFIC weekGb 파라미터 매핑: 0=주간(월~일), 1=주말(금~일), 2=주중(월~목)
    private fun BoxOfficeWeekType.toWeekGb(): String = when (this) {
        BoxOfficeWeekType.WEEK -> "0"
        BoxOfficeWeekType.WEEKEND -> "1"
        BoxOfficeWeekType.WEEKDAY -> "2"
    }

    // KOFIC은 key 오류/파라미터 누락을 HTTP 상태 코드가 아닌 200 OK + faultInfo로 알린다.
    // errorCode가 32로 시작하면 인증키 관련 오류(발급/등록 문제)이므로 Unauthorized로 매핑한다.
    private fun KoficFaultInfo?.toDomainException(): DomainException {
        val cause = IllegalStateException(this?.message ?: "Unknown KOFIC API fault")
        return if (this?.errorCode?.startsWith("32") == true) {
            DomainException.Unauthorized(cause)
        } else {
            DomainException.ServerError(0, cause)
        }
    }

    private companion object {
        val TARGET_DATE_REGEX = Regex("^\\d{8}$")
    }
}
