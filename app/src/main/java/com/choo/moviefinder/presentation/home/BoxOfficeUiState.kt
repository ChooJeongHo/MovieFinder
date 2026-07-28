package com.choo.moviefinder.presentation.home

import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.BoxOfficeMovie

// 기간 전환(일별/주간) 중에도 현재 선택된 기간과 로딩 여부를 동시에 표현해야 하므로
// sealed 계층 대신 단일 data class로 둔다 — 로딩 중에도 선택된 칩을 유지할 수 있다.
data class BoxOfficeUiState(
    val period: BoxOfficePeriod = BoxOfficePeriod.DAILY,
    val isLoading: Boolean = true,
    val items: List<BoxOfficeMovie> = emptyList(),
    val errorType: ErrorType? = null
)
