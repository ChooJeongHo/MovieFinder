package com.choo.moviefinder.data.local.dao

// Room 쿼리 결과를 매핑하기 위한 장르별 시청 편수 POJO
data class GenreCountResult(
    val genre: String,
    val count: Int
)
