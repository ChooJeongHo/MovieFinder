package com.choo.moviefinder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// KMRB 등급 매칭용 한국어 제목 캐시. 앱 언어가 English일 때 화면 표시 title은 영어로 바뀌지만
// KMRB(정부 공공 API)는 한국어 등록명만 갖고 있어, movieId 기준으로 ko-KR 제목을 한 번만 조회해
// 재사용한다 (등급 필터가 켜질 때마다 반복 조회하는 N+1을 피하기 위함).
@Entity(tableName = "movie_korean_title_cache")
data class MovieKoreanTitleCacheEntity(
    @PrimaryKey val movieId: Int,
    val koreanTitle: String,
    val cachedAt: Long
)
