package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.KoreanRating

interface KoreanRatingRepository {

    // KMRB(영상물등급위원회)에서 title로 영화 등급분류정보를 검색한다
    suspend fun searchRatings(title: String, pageNo: Int = 1, numOfRows: Int = 10): List<KoreanRating>
}
