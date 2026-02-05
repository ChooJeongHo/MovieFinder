package com.choo.moviefinder

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.choo.moviefinder.core.util.NetworkMonitor
import com.choo.moviefinder.databinding.ActivityMainBinding
import com.choo.moviefinder.presentation.detail.DetailFragmentArgs
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    private lateinit var binding: ActivityMainBinding
    private var offlineSnackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)

        // 하단 내비게이션 시스템 바 인셋 처리
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = systemBars.bottom)
            insets
        }

        // 상세 화면에서 하단 내비게이션 숨김
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.detailFragment -> binding.bottomNav.visibility = View.GONE
                else -> binding.bottomNav.visibility = View.VISIBLE
            }
        }

        // 콜드 스타트 시 TMDB 웹 딥링크 처리
        if (savedInstanceState == null) {
            handleTmdbDeepLink(intent)
        }

        // 네트워크 연결 상태 감지 → 오프라인 시 Snackbar 표시
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkMonitor.isConnected.collect { isConnected ->
                    if (!isConnected) {
                        offlineSnackbar = Snackbar.make(
                            binding.root,
                            R.string.offline_message,
                            Snackbar.LENGTH_INDEFINITE
                        ).also { it.show() }
                    } else {
                        offlineSnackbar?.dismiss()
                        offlineSnackbar = null
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 커스텀 스킴(moviefinder://)은 Navigation 컴포넌트가 자동 처리
        if (!navController.handleDeepLink(intent)) {
            // TMDB 웹 URL은 수동 파싱 필요
            handleTmdbDeepLink(intent)
        }
    }

    private fun handleTmdbDeepLink(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return

        if (uri.host == "www.themoviedb.org" &&
            uri.pathSegments.size >= 2 &&
            uri.pathSegments[0] == "movie"
        ) {
            // TMDB URL 형식: /movie/550 또는 /movie/550-fight-club
            val movieSegment = uri.pathSegments[1]
            val movieId = movieSegment.split("-").firstOrNull()?.toIntOrNull() ?: return

            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navHostFragment.navController.navigate(
                R.id.detailFragment,
                DetailFragmentArgs(movieId).toBundle()
            )
        }
    }
}
