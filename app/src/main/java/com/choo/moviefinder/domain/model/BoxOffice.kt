package com.choo.moviefinder.domain.model

data class BoxOffice(
    val rank: Int,
    val rankChange: Int,
    val isNewEntry: Boolean,
    val movieCode: String,
    val movieName: String,
    val openDate: String,
    val audienceCount: Long,
    val audienceAccumulate: Long,
    val salesAmount: Long,
    val screenCount: Int
)
