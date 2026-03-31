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
class SearchFragmentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
        // Navigate to search screen
        onView(withId(R.id.searchFragment)).perform(click())
    }

    @Test
    fun searchScreen_displaysSearchInput() {
        onView(withId(R.id.et_search))
            .check(matches(isDisplayed()))
    }

    @Test
    fun searchScreen_displaysSearchInputLayout() {
        onView(withId(R.id.search_input_layout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun searchScreen_displaysFilterChips() {
        onView(withId(R.id.chip_group_filters))
            .check(matches(isDisplayed()))
    }

    @Test
    fun searchScreen_displaysYearFilterChip() {
        onView(withId(R.id.chip_year))
            .check(matches(isDisplayed()))
    }

    @Test
    fun searchScreen_displaysGenreFilterChip() {
        onView(withId(R.id.chip_genre))
            .check(matches(isDisplayed()))
    }
}
