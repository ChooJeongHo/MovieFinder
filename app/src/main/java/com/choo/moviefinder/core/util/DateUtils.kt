package com.choo.moviefinder.core.util

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
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
