package com.choo.moviefinder

import androidx.test.espresso.Espresso.onView
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
}
