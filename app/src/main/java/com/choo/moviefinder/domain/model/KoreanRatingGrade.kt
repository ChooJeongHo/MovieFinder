package com.choo.moviefinder.domain.model

// "전체"는 enum 값이 아니라 null로 표현한다.
// apiGradeName은 KMRB 응답 gradeName 원문과 정확히 일치해야 매칭에 쓸 수 있다.
enum class KoreanRatingGrade(val apiGradeName: String) {
    ALL_AGES("전체관람가"),
    TWELVE_AND_UP("12세이상관람가"),
    FIFTEEN_AND_UP("15세이상관람가"),
    RESTRICTED("청소년관람불가")
}
