package com.choo.moviefinder.domain.model

// KOFIC 박스오피스 순위 정보에 TMDB 메타데이터(포스터/평점)를 매칭 시도한 결과.
// matchedMovie가 null이면 TMDB에서 동일 제목을 찾지 못한 경우이며, UI는 이를 별도로 처리해야 한다.
// koreanRating은 AttachKoreanRatingToBoxOfficeUseCase가 부착하는 KMRB(영상물등급위원회) 관람등급으로,
// matchedMovie와 무관하게(TMDB 매칭 실패해도) boxOffice.movieName 기준으로 조회된다.
data class BoxOfficeMovie(
    val boxOffice: BoxOffice,
    val matchedMovie: Movie?,
    val koreanRating: KoreanRating? = null
)
