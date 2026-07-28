@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.choo.moviefinder.core.util

import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
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
): String = clock.now().toLocalDateTime(timeZone).date.minus(DatePeriod(days = 1)).toKoficDateString()

// KOFIC 주간 박스오피스의 기본 조회일(마지막으로 완료된 주에 속하는 날짜, KST)을 "yyyyMMdd"로 반환한다.
// KOFIC 주간 집계는 월~일 단위이고 진행 중인 주는 집계되지 않으므로, 오늘보다 앞선 가장 최근 일요일을 넘긴다.
// (오늘이 일요일이면 그 주는 아직 집계 전이므로 7일 전 일요일 = 지난주가 선택된다)
fun koficDefaultWeeklyTargetDate(
    clock: Clock = Clock.System,
    timeZone: TimeZone = TimeZone.of("Asia/Seoul")
): String {
    val today = clock.now().toLocalDateTime(timeZone).date
    // ISO 요일 번호(월=1 ... 일=7)만큼 빼면 항상 오늘보다 앞선 가장 최근 일요일이 된다
    val lastSunday = today.minus(DatePeriod(days = today.dayOfWeek.isoDayNumber))
    return lastSunday.toKoficDateString()
}

// KOFIC targetDt 형식("yyyyMMdd")으로 변환한다
private fun LocalDate.toKoficDateString(): String {
    val month = monthNumber.toString().padStart(2, '0')
    val day = day.toString().padStart(2, '0')
    return "$year$month$day"
}
