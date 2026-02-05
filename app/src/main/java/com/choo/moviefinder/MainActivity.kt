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
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.choo.moviefinder.databinding.ActivityMainBinding
import com.choo.moviefinder.presentation.detail.DetailFragmentArgs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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

        // Handle window insets for bottom nav
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = systemBars.bottom)
            insets
        }

        // Hide bottom nav on detail screen
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.detailFragment -> binding.bottomNav.visibility = View.GONE
                else -> binding.bottomNav.visibility = View.VISIBLE
            }
        }

        // Handle TMDB web deep links on cold start
        if (savedInstanceState == null) {
            handleTmdbDeepLink(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Custom scheme (moviefinder://) is handled by Navigation component
        if (!navController.handleDeepLink(intent)) {
            // TMDB web URL needs manual parsing
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
            // TMDB URL format: /movie/550 or /movie/550-fight-club
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
