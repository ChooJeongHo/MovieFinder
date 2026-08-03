package com.choo.moviefinder.presentation.widget

import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Glance state(Preferences)에 저장된 스냅샷을 읽는 경로 검증.
// Preferences는 순수 JVM 타입이라 Android 런타임 없이 테스트 가능하다.
class BoxOfficeWidgetStateTest {

    @Test
    fun `readSnapshot decodes stored snapshot`() {
        val snapshot = BoxOfficeWidgetSnapshot(
            items = listOf(BoxOfficeWidgetItem(rank = 1, movieName = "영화 1", audienceCount = 100L, tmdbId = 550)),
            fetchedAtMillis = 11L
        )
        val preferences = mutablePreferencesOf(
            BoxOfficeWidget.KEY_SNAPSHOT to boxOfficeWidgetJson.encodeToString(snapshot)
        )

        assertEquals(snapshot, preferences.readSnapshot())
    }

    @Test
    fun `readSnapshot returns null when nothing stored yet`() {
        assertNull(mutablePreferencesOf().readSnapshot())
    }

    @Test
    fun `readSnapshot returns null instead of crashing on corrupted payload`() {
        val preferences = mutablePreferencesOf(BoxOfficeWidget.KEY_SNAPSHOT to "{not-json")

        assertNull(preferences.readSnapshot())
    }
}
