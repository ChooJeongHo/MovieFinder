package com.choo.moviefinder.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE = "com.choo.moviefinder"

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        rule.collect(packageName = "com.choo.moviefinder") {
            // 1. 콜드 스타트
            pressHome()
            startActivityAndWait()
            device.waitForIdle(3000)

            // 2. 홈 화면 스크롤
            scrollHomeScreen()

            // 3. 검색 화면
            navigateToSearch()

            // 4. 즐겨찾기 화면
            navigateToFavorites()

            // 5. 통계 화면 (딥링크)
            navigateToStats()

            // 6. 영화 상세 화면 (딥링크)
            navigateToMovieDetail()
        }
    }

    private fun MacrobenchmarkScope.scrollHomeScreen() {
        val recyclerView = device.findObject(By.scrollable(true))
        recyclerView?.fling(Direction.DOWN)
        device.waitForIdle(1000)
        recyclerView?.fling(Direction.UP)
        device.waitForIdle(1000)
    }

    private fun MacrobenchmarkScope.navigateToSearch() {
        val searchNav = device.findObject(By.desc("검색").clickable(true))
            ?: device.findObject(By.res("com.choo.moviefinder:id/navigation_search"))
        searchNav?.click()
        device.waitForIdle(2000)

        // 검색어 입력
        val searchInput = device.findObject(By.res("com.choo.moviefinder:id/etSearch"))
        searchInput?.click()
        searchInput?.text = "action"
        device.waitForIdle(2000)

        // 결과 스크롤
        val results = device.findObject(By.scrollable(true))
        results?.fling(Direction.DOWN)
        device.waitForIdle(1000)
    }

    private fun MacrobenchmarkScope.navigateToFavorites() {
        val favNav = device.findObject(By.desc("즐겨찾기").clickable(true))
            ?: device.findObject(By.res("com.choo.moviefinder:id/navigation_favorite"))
        favNav?.click()
        device.waitForIdle(2000)
    }

    private fun MacrobenchmarkScope.navigateToStats() {
        device.executeShellCommand(
            "am start -a android.intent.action.VIEW -d moviefinder://stats $PACKAGE"
        )
        device.wait(Until.hasObject(By.pkg(PACKAGE)), 3000)
        device.waitForIdle(2000)

        val scrollable = device.findObject(By.scrollable(true))
        scrollable?.fling(Direction.DOWN)
        device.waitForIdle(1000)
    }

    private fun MacrobenchmarkScope.navigateToMovieDetail() {
        device.executeShellCommand(
            "am start -a android.intent.action.VIEW -d moviefinder://movie/550 $PACKAGE"
        )
        device.wait(Until.hasObject(By.pkg(PACKAGE)), 3000)
        device.waitForIdle(2000)

        val scrollable = device.findObject(By.scrollable(true))
        scrollable?.fling(Direction.DOWN)
        device.waitForIdle(1000)
        scrollable?.fling(Direction.DOWN)
        device.waitForIdle(1000)
    }
}
