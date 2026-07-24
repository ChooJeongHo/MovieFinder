package com.choo.moviefinder.data.remote.dto

import com.choo.moviefinder.domain.model.BoxOffice
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// KOFIC은 key 누락/오류, 필수 파라미터 누락 등을 HTTP 상태 코드가 아니라 boxOfficeResult 없이
// faultInfo만 담긴 200 OK 응답으로 알린다 — 두 필드 모두 nullable로 두고 응답 형태로 성공/실패를 구분한다.
@Serializable
data class KoficBoxOfficeResponse(
    @SerialName("boxOfficeResult") val boxOfficeResult: KoficBoxOfficeResult? = null,
    @SerialName("faultInfo") val faultInfo: KoficFaultInfo? = null
)

@Serializable
data class KoficBoxOfficeResult(
    @SerialName("dailyBoxOfficeList") val dailyBoxOfficeList: List<KoficDailyBoxOfficeDto> = emptyList()
)

@Serializable
data class KoficFaultInfo(
    @SerialName("message") val message: String = "",
    @SerialName("errorCode") val errorCode: String = ""
)

// KOFIC은 모든 필드를 문자열로 반환한다 (숫자 필드도 예외 아님)
@Serializable
data class KoficDailyBoxOfficeDto(
    @SerialName("rank") val rank: String,
    @SerialName("rankInten") val rankInten: String = "0",
    @SerialName("rankOldAndNew") val rankOldAndNew: String = "OLD",
    @SerialName("movieCd") val movieCd: String,
    @SerialName("movieNm") val movieNm: String,
    @SerialName("openDt") val openDt: String = "",
    @SerialName("audiCnt") val audiCnt: String = "0",
    @SerialName("audiAcc") val audiAcc: String = "0",
    @SerialName("salesAmt") val salesAmt: String = "0",
    @SerialName("scrnCnt") val scrnCnt: String = "0"
)

// KOFIC DTO를 도메인 BoxOffice 모델로 변환 (모든 수치 필드는 문자열 → Long/Int 파싱)
fun KoficDailyBoxOfficeDto.toDomain() = BoxOffice(
    rank = rank.toIntOrNull() ?: 0,
    rankChange = rankInten.toIntOrNull() ?: 0,
    isNewEntry = rankOldAndNew == "NEW",
    movieCode = movieCd,
    movieName = movieNm,
    openDate = openDt,
    audienceCount = audiCnt.toLongOrNull() ?: 0L,
    audienceAccumulate = audiAcc.toLongOrNull() ?: 0L,
    salesAmount = salesAmt.toLongOrNull() ?: 0L,
    screenCount = scrnCnt.toIntOrNull() ?: 0
)
