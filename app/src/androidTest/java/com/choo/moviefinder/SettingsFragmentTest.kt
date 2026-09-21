package com.choo.moviefinder

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SettingsFragmentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
        // Navigate to settings screen
        onView(withId(R.id.settingsFragment)).perform(click())
    }

    @Test
    fun settingsScreen_displaysToolbar() {
        onView(withId(R.id.toolbar))
            .check(matches(isDisplayed()))
    }

    @Test
    fun settingsScreen_displaysThemeOption() {
        onView(withId(R.id.item_theme))
            .check(matches(isDisplayed()))
    }

    @Test
    fun settingsScreen_displaysCacheClearOption() {
        onView(withId(R.id.item_clear_cache))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun settingsScreen_displaysWatchHistoryClearOption() {
        onView(withId(R.id.item_clear_watch_history))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun settingsScreen_displaysExportDataOption() {
        onView(withId(R.id.item_export_data))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun settingsScreen_displaysImportDataOption() {
        onView(withId(R.id.item_import_data))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun settingsScreen_displaysVersionInfo() {
        onView(withId(R.id.tv_app_version))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun settingsScreen_displaysStatsNavigation() {
        onView(withId(R.id.item_stats))
            .check(matches(isDisplayed()))
    }

    // 언어 변경은 Activity recreate를 유발하므로 각 테스트 후 시스템 기본값으로 복원
    @After
    fun resetLanguage() {
        activityRule.scenario.onActivity {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }
    }

    @Test
    fun settingsScreen_languageDialog_showsThreeOptions() {
        onView(withId(R.id.item_language)).perform(scrollTo(), click())

        onView(withText(R.string.language_system)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(R.string.language_korean)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(R.string.language_english)).inRoot(isDialog()).check(matches(isDisplayed()))
    }

    @Test
    fun settingsScreen_selectEnglish_appliesLocaleAndUpdatesValueLabel() {
        onView(withId(R.id.item_language)).perform(scrollTo(), click())
        onView(withText(R.string.language_english)).inRoot(isDialog()).perform(click())

        // recreate 이후에도 앱 로케일이 en으로 유지되고, 표시 값이 English여야 한다
        assertEquals("en", AppCompatDelegate.getApplicationLocales().toLanguageTags())
        onView(withId(R.id.tv_language_value))
            .perform(scrollTo())
            .check(matches(withText(R.string.language_english)))
    }

    @Test
    fun settingsScreen_selectSystem_clearsApplicationLocales() {
        onView(withId(R.id.item_language)).perform(scrollTo(), click())
        onView(withText(R.string.language_korean)).inRoot(isDialog()).perform(click())
        assertEquals("ko", AppCompatDelegate.getApplicationLocales().toLanguageTags())

        onView(withId(R.id.item_language)).perform(scrollTo(), click())
        onView(withText(R.string.language_system)).inRoot(isDialog()).perform(click())

        assertTrue(AppCompatDelegate.getApplicationLocales().isEmpty)
    }

    // action_settings_to_stats의 Animator 기반 전환(Predictive Back 전환용) 왕복 회귀 테스트
    @Test
    fun settingsScreen_navigateToStats_backReturnsToSettings() {
        onView(withId(R.id.item_stats)).perform(click())
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()))

        pressBack()

        onView(withId(R.id.item_stats)).check(matches(isDisplayed()))
    }
}
