package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.KoreanRating

interface KoreanRatingRepository {

    // KMRB(영상물등급위원회)에서 title로 영화 등급분류정보를 검색한다
    suspend fun searchRatings(title: String, pageNo: Int = 1, numOfRows: Int = 10): List<KoreanRating>

    // 캐시 우선. 반환 null은 "등급 정보 없음"이며 조회 실패와 구분되지 않는다(실패는 예외로 전파).
    suspend fun getRatingForMovie(movieTitle: String): KoreanRating?
}
