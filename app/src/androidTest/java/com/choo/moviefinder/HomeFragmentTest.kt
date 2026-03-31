package com.choo.moviefinder

import androidx.test.espresso.Espresso.onView
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
class HomeFragmentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun homeScreen_displaysTabLayout() {
        onView(withId(R.id.tab_layout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun homeScreen_displaysToolbar() {
        onView(withId(R.id.toolbar))
            .check(matches(isDisplayed()))
    }

    @Test
    fun homeScreen_displaysRecyclerView() {
        onView(withId(R.id.rv_movies))
            .check(matches(isDisplayed()))
    }

    @Test
    fun homeScreen_displaysSwipeRefresh() {
        onView(withId(R.id.swipe_refresh))
            .check(matches(isDisplayed()))
    }

    @Test
    fun homeScreen_displaysScrollToTopFabArea() {
        onView(withId(R.id.fab_scroll_top))
            .check(matches(withId(R.id.fab_scroll_top)))
    }
}
