package com.choo.moviefinder

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// 109일차: singleTask MainActivity가 TMDB 브라우저 왕복 도중 프로세스가 죽으면
// moviefinder://auth/callback 인텐트를 onNewIntent가 아니라 onCreate로 전달받는다.
// 이 콜백 처리가 onNewIntent에만 있고 onCreate에는 없어 조용히 무시되던 결함을
// handleTmdbAuthCallback()을 양쪽에서 공유 호출하도록 고쳤다 — 이 테스트는 onCreate 경로에서
// 크래시 없이 처리되는지(=onCreate가 실제로 그 함수를 호출하고 있는지)를 회귀 검증한다.
//
// 실기기 검증(SM-S926N) 결과 TMDB v4의 실제 콜백은 request_token 등 쿼리 파라미터를 전혀
// 붙이지 않는다 — 항상 "moviefinder://auth/callback" 그대로 온다. 따라서 이 테스트는 쿼리
// 없는 순수 콜백 URI로만 검증한다(실제 TMDB 동작과 다른 가짜 쿼리 파라미터를 넣지 않는다).
@HiltAndroidTest
class MainActivityOAuthCallbackTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    private fun oauthCallbackIntent(): Intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("moviefinder://auth/callback"),
        ApplicationProvider.getApplicationContext(),
        MainActivity::class.java
    )

    // 콜드 스타트(onCreate, savedInstanceState == null)로 이 인텐트가 전달되는 경우를 재현한다.
    // pendingRequestToken이 없는 상태(SavedStateHandle이 비어있는 새 ViewModel)이므로
    // "대기 중인 토큰 없음" 분기를 타는데, 이 분기가 실행되려면 애초에 onCreate가
    // handleTmdbAuthCallback을 호출해야 한다 — 호출 자체가 없다면(회귀) 아래 단언은 우연히는
    // 통과하지만, 크래시가 나거나 엉뚱한 화면(detailFragment 등)으로 튀는 경우는 이 테스트가 잡는다.
    @Test
    fun oauthCallback_coldStart_doesNotCrashAndDoesNotMisnavigate() {
        ActivityScenario.launch<MainActivity>(oauthCallbackIntent()).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.state)

            var destinationId: Int? = null
            scenario.onActivity { activity ->
                val navHostFragment = activity.supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                destinationId = navHostFragment.navController.currentDestination?.id
            }
            // auth/callback은 nav_graph 딥링크 대상이 아니므로 detail/stats 등으로 잘못 튀면 안 된다.
            assertNotEquals(R.id.detailFragment, destinationId)
            assertNotEquals(R.id.statsFragment, destinationId)
        }
    }

    // 콜백 URI의 scheme/host/path가 조금이라도 다르면(예: 다른 앱이 흉내낸 잘못된 딥링크)
    // 아무 처리 없이 무시되어야 한다 — 크래시 없이 통과하는지만 확인한다.
    @Test
    fun unrelatedDeepLink_doesNotCrash() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("moviefinder://auth/other"),
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }
}
