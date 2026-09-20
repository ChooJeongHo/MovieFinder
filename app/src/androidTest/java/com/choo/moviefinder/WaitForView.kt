package com.choo.moviefinder

import android.os.SystemClock
import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId

private const val DEFAULT_TIMEOUT_MILLIS = 10_000L
private const val POLL_INTERVAL_MILLIS = 250L

// 뷰가 화면에 나타날 때까지 폴링한다.
// Espresso는 메인 스레드 idle만 기다리므로, 네트워크/Room 캐시 로딩이 끝나야 보이는 뷰
// (예: 데이터가 도착해야 VISIBLE이 되는 rv_movies)는 IdlingResource 없이는 직접 기다려야 한다.
// 시간 안에 나타나지 않으면 마지막에 한 번 더 검사해 Espresso의 원래 실패 메시지를 그대로 던진다.
fun waitUntilDisplayed(
    @IdRes viewId: Int,
    timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS
) {
    val deadline = SystemClock.uptimeMillis() + timeoutMillis
    while (SystemClock.uptimeMillis() < deadline) {
        if (isDisplayedNow(viewId)) return
        SystemClock.sleep(POLL_INTERVAL_MILLIS)
    }
    onView(withId(viewId)).check(matches(isDisplayed()))
}

private fun isDisplayedNow(@IdRes viewId: Int): Boolean = try {
    onView(withId(viewId)).check(matches(isDisplayed()))
    true
} catch (_: AssertionError) {
    false
} catch (_: NoMatchingViewException) {
    false
}
