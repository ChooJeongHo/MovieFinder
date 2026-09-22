package com.choo.moviefinder

import android.os.Build
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// MovieFinderApp.onCreate()의 DynamicColors.applyToActivitiesIfAvailable() 등록이
// 앱 시작을 깨뜨리지 않고, API 레벨 게이팅이 문서화된 동작과 일치하는지 검증한다.
@HiltAndroidTest
class MainActivityDynamicColorTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun dynamicColorAvailability_matchesApi31Gate() {
        val expectedAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        assertEquals(expectedAvailable, DynamicColors.isDynamicColorAvailable())
    }

    @Test
    fun mainActivity_launchesWithDynamicColorsApplied() {
        onView(withId(R.id.bottom_nav))
            .check(matches(isDisplayed()))
    }
}
