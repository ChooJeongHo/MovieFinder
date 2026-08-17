package com.choo.moviefinder.data.remote.dto

import com.choo.moviefinder.domain.model.KoreanRating

// KMRB는 XML 응답이며 봉투 구조(response/body/items)가 미확정이라 SAX 파서가 직접 채운다.
// 모든 필드를 기본값으로 두어 일부 태그가 누락돼도 예외 없이 파싱된다.
data class KmrbRatingItemDto(
    val gradeName: String = "",
    val useTitle: String = "",
    val prodcName: String = "",
    val directorName: String = "",
    val prodYear: String = "",
    val majorOpinNarrnCont: String = ""
)

data class KmrbSearchResult(
    val resultCode: String? = null,
    val resultMsg: String? = null,
    val items: List<KmrbRatingItemDto> = emptyList()
)

fun KmrbRatingItemDto.toDomain(): KoreanRating = KoreanRating(
    gradeName = gradeName,
    title = useTitle,
    productionCompany = prodcName,
    directorName = directorName,
    productionYear = prodYear,
    ratingReason = majorOpinNarrnCont
)
