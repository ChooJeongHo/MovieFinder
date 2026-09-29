package com.choo.moviefinder.domain.repository

// 검색 결과처럼 ko-KR로 고정 조회한 목록(MoviePagingSource 참고)의 "화면 표시용" 제목을 현재 앱 언어로 바꿔 준다.
// 필터(KMRB 매칭)는 ko-KR title이 필요하므로 목록 데이터 자체는 건드리지 않고, 화면에 보이는 항목의
// 표시 문자열만 지연 조회하는 것이 목적이다. 필터 판정에는 전체 항목이 필요하지만 표시 제목은 그렇지 않다.
interface LocalizedTitleRepository {

    // ko-KR 목록 제목을 앱 언어로 다시 조회해야 하는지 (앱 언어가 한국어면 false — 이미 한국어 제목)
    fun needsLocalization(): Boolean

    // 이미 조회해 캐시에 있는 표시 제목을 네트워크 없이 즉시 반환한다. 캐시 미스면 null
    fun peekTitle(movieId: Int): String?

    // 앱 언어로 표시 제목을 조회한다 (캐시 우선, 동시 요청 수 제한). 실패 시 DomainException을 던진다
    suspend fun getTitle(movieId: Int): String
}
