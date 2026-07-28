@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.choo.moviefinder.core.util

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateUtilsTest {

    // 2024-06-15T12:00:00Z
    private val juneInstant = Instant.fromEpochMilliseconds(1718445600000L)

    // 2024-01-15T12:00:00Z
    private val januaryInstant = Instant.fromEpochMilliseconds(1705320000000L)

    // 2024-12-05T12:00:00Z
    private val decemberInstant = Instant.fromEpochMilliseconds(1733392800000L)

    private fun fixedClock(instant: Instant) = object : Clock {
        override fun now() = instant
    }

    @Test
    fun `currentMonthStartMillis - returns first day of month at midnight UTC`() {
        val result = currentMonthStartMillis(fixedClock(juneInstant), TimeZone.UTC)
        val resultDate = Instant.fromEpochMilliseconds(result).toLocalDateTime(TimeZone.UTC)

        assertEquals(2024, resultDate.year)
        assertEquals(6, resultDate.monthNumber)
        assertEquals(1, resultDate.dayOfMonth)
        assertEquals(0, resultDate.hour)
        assertEquals(0, resultDate.minute)
        assertEquals(0, resultDate.second)
    }

    @Test
    fun `currentMonthStartMillis - result is less than or equal to current instant`() {
        val result = currentMonthStartMillis(fixedClock(juneInstant), TimeZone.UTC)
        assertTrue(result <= juneInstant.toEpochMilliseconds())
    }

    @Test
    fun `currentMonthStartMillis - january returns first of january`() {
        val result = currentMonthStartMillis(fixedClock(januaryInstant), TimeZone.UTC)
        val resultDate = Instant.fromEpochMilliseconds(result).toLocalDateTime(TimeZone.UTC)

        assertEquals(1, resultDate.monthNumber)
        assertEquals(1, resultDate.dayOfMonth)
    }

    @Test
    fun `currentYearMonth - returns YYYY-MM format`() {
        val result = currentYearMonth(fixedClock(juneInstant), TimeZone.UTC)
        assertEquals("2024-06", result)
    }

    @Test
    fun `currentYearMonth - january padded to two digits`() {
        val result = currentYearMonth(fixedClock(januaryInstant), TimeZone.UTC)
        assertEquals("2024-01", result)
    }

    @Test
    fun `currentYearMonth - december returns 12`() {
        val result = currentYearMonth(fixedClock(decemberInstant), TimeZone.UTC)
        assertEquals("2024-12", result)
    }

    @Test
    fun `currentYearMonth - format always has hyphen and two-digit month`() {
        val result = currentYearMonth(fixedClock(januaryInstant), TimeZone.UTC)
        assertTrue(result.matches(Regex("\\d{4}-\\d{2}")))
    }

    // ── KOFIC 조회 기준일 ────────────────────────────────────

    @Test
    fun `koficDefaultTargetDate - returns previous day in yyyyMMdd`() {
        // 2024-06-15T12:00Z → UTC 기준 어제는 2024-06-14
        val result = koficDefaultTargetDate(fixedClock(juneInstant), TimeZone.UTC)
        assertEquals("20240614", result)
    }

    // 2024-06-15는 토요일 → 오늘보다 앞선 가장 최근 일요일은 2024-06-09
    @Test
    fun `koficDefaultWeeklyTargetDate - saturday returns most recent past sunday`() {
        val result = koficDefaultWeeklyTargetDate(fixedClock(juneInstant), TimeZone.UTC)
        assertEquals("20240609", result)
    }

    // 오늘이 일요일이면 그 주는 아직 집계 전이므로 7일 전(지난주 일요일)을 반환해야 한다
    @Test
    fun `koficDefaultWeeklyTargetDate - sunday returns previous sunday not today`() {
        // 2024-06-16T12:00:00Z = 일요일
        val sundayInstant = Instant.fromEpochMilliseconds(1718539200000L)
        val result = koficDefaultWeeklyTargetDate(fixedClock(sundayInstant), TimeZone.UTC)
        assertEquals("20240609", result)
    }

    // 2024-06-17은 월요일 → 바로 전날인 2024-06-16 일요일
    @Test
    fun `koficDefaultWeeklyTargetDate - monday returns yesterday sunday`() {
        // 2024-06-17T12:00:00Z = 월요일
        val mondayInstant = Instant.fromEpochMilliseconds(1718625600000L)
        val result = koficDefaultWeeklyTargetDate(fixedClock(mondayInstant), TimeZone.UTC)
        assertEquals("20240616", result)
    }

    // 월 경계를 넘어가는 경우에도 zero-padding이 유지돼야 한다 (2024-01-15 월요일 → 2024-01-14 일요일)
    @Test
    fun `koficDefaultWeeklyTargetDate - pads month and day to two digits`() {
        val result = koficDefaultWeeklyTargetDate(fixedClock(januaryInstant), TimeZone.UTC)
        assertEquals("20240114", result)
        assertTrue(result.matches(Regex("^\\d{8}$")))
    }

    // 항상 오늘보다 앞선 날짜여야 한다 (진행 중인 주를 조회하면 KOFIC이 빈 결과를 준다)
    @Test
    fun `koficDefaultWeeklyTargetDate - result is always strictly before today`() {
        listOf(juneInstant, januaryInstant, decemberInstant).forEach { instant ->
            val weekly = koficDefaultWeeklyTargetDate(fixedClock(instant), TimeZone.UTC)
            val today = instant.toLocalDateTime(TimeZone.UTC).date
            val todayString = "%04d%02d%02d".format(today.year, today.monthNumber, today.day)
            assertTrue("weekly=$weekly today=$todayString", weekly < todayString)
        }
    }
}
