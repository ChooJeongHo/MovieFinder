@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.choo.moviefinder.core.util

import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

// 현재 월의 시작 시점을 epoch milliseconds로 반환한다
fun currentMonthStartMillis(
    clock: Clock = Clock.System,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Long {
    val localDate = clock.now().toLocalDateTime(timeZone).date
    val monthStart = LocalDate(localDate.year, localDate.month, 1)
    return monthStart.atStartOfDayIn(timeZone).toEpochMilliseconds()
}

// 현재 연-월을 "YYYY-MM" 형식 문자열로 반환한다
fun currentYearMonth(
    clock: Clock = Clock.System,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String {
    val localDate = clock.now().toLocalDateTime(timeZone).date
    return "${localDate.year}-${localDate.monthNumber.toString().padStart(2, '0')}"
}

// KOFIC 일별 박스오피스의 기본 조회일(전일, KST)을 "yyyyMMdd" 형식으로 반환한다.
// KOFIC 통계는 당일 자정 무렵에야 집계가 완료되므로 "오늘"을 넘기면 빈 응답이 돌아온다.
fun koficDefaultTargetDate(
    clock: Clock = Clock.System,
    timeZone: TimeZone = TimeZone.of("Asia/Seoul")
): String {
    val yesterday = clock.now().toLocalDateTime(timeZone).date.minus(DatePeriod(days = 1))
    val month = yesterday.monthNumber.toString().padStart(2, '0')
    val day = yesterday.day.toString().padStart(2, '0')
    return "${yesterday.year}$month$day"
}
