package com.choo.moviefinder.data.local.dao

// Room 쿼리 결과를 매핑하기 위한 월별 시청 편수 POJO
data class MonthlyCount(
    val yearMonth: String,
    val count: Int
)
