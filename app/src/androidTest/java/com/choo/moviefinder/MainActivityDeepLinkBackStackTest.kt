package com.choo.moviefinder

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// 103일차에 도입된 normalizeShortcutEntryBackStack()이 search/favorite 딥링크만 커버하고
// movie(detailFragment)/stats(statsFragment) 딥링크는 빠뜨려, 콜드 스타트 시 합성 백스택에
// 남은 onboardingFragment로 뒤로가기가 역류하던 회귀를 검증한다.
// search/favorite 테스트는 103일차에 이미 고쳤던 경로가 이번 변경으로 깨지지 않았는지 재확인하는 회귀 테스트다.
@HiltAndroidTest
class MainActivityDeepLinkBackStackTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    private fun deepLinkIntent(uri: String): Intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse(uri),
        ApplicationProvider.getApplicationContext(),
        MainActivity::class.java
    )

    private fun currentDestinationId(scenario: ActivityScenario<MainActivity>): Int? {
        var destinationId: Int? = null
        scenario.onActivity { activity ->
            val navHostFragment = activity.supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            destinationId = navHostFragment.navController.currentDestination?.id
        }
        return destinationId
    }

    @Test
    fun movieDeepLink_backPress_finishesInsteadOfReturningToOnboarding() {
        ActivityScenario.launch<MainActivity>(deepLinkIntent("moviefinder://movie/550")).use { scenario ->
            assertEquals(R.id.detailFragment, currentDestinationId(scenario))

            pressBack()

            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }

    @Test
    fun statsDeepLink_backPress_finishesInsteadOfReturningToOnboarding() {
        ActivityScenario.launch<MainActivity>(deepLinkIntent("moviefinder://stats")).use { scenario ->
            assertEquals(R.id.statsFragment, currentDestinationId(scenario))

            pressBack()

            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }

    @Test
    fun searchShortcutDeepLink_backPress_stillFinishes_regression() {
        ActivityScenario.launch<MainActivity>(deepLinkIntent("moviefinder://search")).use { scenario ->
            assertEquals(R.id.searchFragment, currentDestinationId(scenario))

            pressBack()

            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }

    @Test
    fun favoriteShortcutDeepLink_backPress_stillFinishes_regression() {
        ActivityScenario.launch<MainActivity>(deepLinkIntent("moviefinder://favorite")).use { scenario ->
            assertEquals(R.id.favoriteFragment, currentDestinationId(scenario))

            pressBack()

            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }
}
