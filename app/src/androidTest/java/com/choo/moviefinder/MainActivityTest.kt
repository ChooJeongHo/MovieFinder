package com.choo.moviefinder

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
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
class MainActivityTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun bottomNavigation_isDisplayed() {
        onView(withId(R.id.bottom_nav))
            .check(matches(isDisplayed()))
    }

    @Test
    fun homeScreen_showsTabLayout() {
        onView(withId(R.id.tab_layout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun navigateToSearch_showsSearchInput() {
        onView(withId(R.id.searchFragment)).perform(click())
        onView(withId(R.id.et_search))
            .check(matches(isDisplayed()))
    }

    @Test
    fun navigateToFavorites_showsTabLayout() {
        onView(withId(R.id.favoriteFragment)).perform(click())
        onView(withId(R.id.tab_layout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun navigateToSettings_showsToolbar() {
        onView(withId(R.id.settingsFragment)).perform(click())
        onView(withId(R.id.toolbar))
            .check(matches(isDisplayed()))
    }
}
