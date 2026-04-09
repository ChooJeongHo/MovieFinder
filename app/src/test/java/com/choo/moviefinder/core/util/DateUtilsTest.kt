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
}
