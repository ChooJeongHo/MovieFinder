package com.choo.moviefinder.presentation.widget

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.choo.moviefinder.presentation.home.BoxOfficePeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Glance state(Preferences)에 저장된 스냅샷/기간을 읽는 경로 검증.
// Preferences는 순수 JVM 타입이라 Android 런타임 없이 테스트 가능하다.
class BoxOfficeWidgetStateTest {

    @Test
    fun `readSnapshot decodes stored snapshot for the given period`() {
        val snapshot = BoxOfficeWidgetSnapshot(
            items = listOf(BoxOfficeWidgetItem(rank = 1, movieName = "영화 1", audienceCount = 100L, tmdbId = 550)),
            fetchedAtMillis = 11L
        )
        val preferences = mutablePreferencesOf(
            BoxOfficeWidget.KEY_SNAPSHOT_DAILY to boxOfficeWidgetJson.encodeToString(snapshot)
        )

        assertEquals(snapshot, preferences.readSnapshot(BoxOfficePeriod.DAILY))
        assertNull(preferences.readSnapshot(BoxOfficePeriod.WEEKLY))
    }

    @Test
    fun `readSnapshot keeps daily and weekly caches independent`() {
        val dailySnapshot = BoxOfficeWidgetSnapshot(
            items = listOf(BoxOfficeWidgetItem(rank = 1, movieName = "일별 1위", audienceCount = 100L)),
            fetchedAtMillis = 1L
        )
        val weeklySnapshot = BoxOfficeWidgetSnapshot(
            items = listOf(BoxOfficeWidgetItem(rank = 1, movieName = "주간 1위", audienceCount = 200L)),
            fetchedAtMillis = 2L
        )
        val preferences = mutablePreferencesOf(
            BoxOfficeWidget.KEY_SNAPSHOT_DAILY to boxOfficeWidgetJson.encodeToString(dailySnapshot),
            BoxOfficeWidget.KEY_SNAPSHOT_WEEKLY to boxOfficeWidgetJson.encodeToString(weeklySnapshot)
        )

        assertEquals(dailySnapshot, preferences.readSnapshot(BoxOfficePeriod.DAILY))
        assertEquals(weeklySnapshot, preferences.readSnapshot(BoxOfficePeriod.WEEKLY))
    }

    @Test
    fun `readSnapshot returns null when nothing stored yet`() {
        assertNull(mutablePreferencesOf().readSnapshot(BoxOfficePeriod.DAILY))
    }

    @Test
    fun `readSnapshot returns null instead of crashing on corrupted payload`() {
        val preferences = mutablePreferencesOf(BoxOfficeWidget.KEY_SNAPSHOT_DAILY to "{not-json")

        assertNull(preferences.readSnapshot(BoxOfficePeriod.DAILY))
    }

    @Test
    fun `readPeriod defaults to DAILY when nothing stored yet`() {
        assertEquals(BoxOfficePeriod.DAILY, mutablePreferencesOf().readPeriod())
    }

    @Test
    fun `readPeriod returns the stored period`() {
        val preferences = mutablePreferencesOf(BoxOfficeWidget.KEY_PERIOD to BoxOfficePeriod.WEEKLY.name)

        assertEquals(BoxOfficePeriod.WEEKLY, preferences.readPeriod())
    }

    @Test
    fun `readPeriod falls back to DAILY on an unrecognized stored value`() {
        val preferences = mutablePreferencesOf(BoxOfficeWidget.KEY_PERIOD to "MONTHLY")

        assertEquals(BoxOfficePeriod.DAILY, preferences.readPeriod())
    }
}
