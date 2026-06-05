package com.choo.moviefinder.data.util

import androidx.paging.PagingConfig

object Constants {
    const val PAGE_SIZE = 20
    const val PREFETCH_DISTANCE = 5

    // API 언어 설정
    const val LANGUAGE_KO = "ko-KR"

    // 예고편 검색 시 영어로 요청하여 더 많은 결과를 얻기 위함
    const val LANGUAGE_EN = "en-US"

    // 시청 기록 최대 표시 개수
    const val WATCH_HISTORY_LIMIT = 20

    // TMDB API 응답 필터링 값 (ISO 3166-1 지역 코드, 크레딧 직책, 영상 사이트/타입)
    const val REGION_KR = "KR"
    const val REGION_US = "US"
    const val CREW_JOB_DIRECTOR = "Director"
    const val VIDEO_SITE_YOUTUBE = "YouTube"
    const val VIDEO_TYPE_TRAILER = "Trailer"

    val DEFAULT_PAGING_CONFIG = PagingConfig(
        pageSize = PAGE_SIZE,
        prefetchDistance = PREFETCH_DISTANCE,
        initialLoadSize = PAGE_SIZE * 3,
        enablePlaceholders = false
    )
}