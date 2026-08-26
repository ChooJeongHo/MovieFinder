package com.choo.moviefinder.domain.model

// "전체"는 enum 값이 아니라 null로 표현한다.
// apiGradeName은 KMRB 응답 gradeName 원문과 정확히 일치해야 매칭에 쓸 수 있다.
enum class KoreanRatingGrade(val apiGradeName: String) {
    ALL_AGES("전체관람가"),
    TWELVE_AND_UP("12세이상관람가"),
    FIFTEEN_AND_UP("15세이상관람가"),
    RESTRICTED("청소년관람불가");

    companion object {
        // KMRB gradeName 원문(API 응답)을 enum으로 역매핑한다. 알려진 4종 외 값은 null을 반환하며,
        // 호출부는 null일 때 원문 문자열로 폴백해야 한다(정보 손실 방지).
        fun fromApiGradeName(gradeName: String): KoreanRatingGrade? =
            entries.firstOrNull { it.apiGradeName == gradeName }
    }
}
