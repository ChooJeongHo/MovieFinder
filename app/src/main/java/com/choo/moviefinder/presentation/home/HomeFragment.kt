package com.choo.moviefinder.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import com.choo.moviefinder.R
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.databinding.FragmentHomeBinding
import com.choo.moviefinder.domain.model.ThemeMode
import com.choo.moviefinder.presentation.adapter.MovieLoadStateAdapter
import com.choo.moviefinder.presentation.adapter.MoviePagingAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var movieAdapter: MoviePagingAdapter
    private var currentTab = 0
    private var collectJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbarMenu()
        setupRecyclerView()
        setupTabs()
        observeLoadStates()
        collectMovies()
    }

    private fun setupToolbarMenu() {
        binding.toolbar.inflateMenu(R.menu.menu_home)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_theme -> {
                    showThemeDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun showThemeDialog() {
        val themes = arrayOf(
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
            getString(R.string.theme_system)
        )
        val currentIndex = viewModel.currentThemeMode.value.ordinal

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.theme_settings)
            .setSingleChoiceItems(themes, currentIndex) { dialog, which ->
                val selectedMode = ThemeMode.entries[which]
                viewModel.setThemeMode(selectedMode)
                dialog.dismiss()
            }
            .show()
    }

    private fun setupRecyclerView() {
        movieAdapter = MoviePagingAdapter { movieId, posterView ->
            if (findNavController().currentDestination?.id == R.id.homeFragment) {
                val action = HomeFragmentDirections.actionHomeToDetail(movieId)
                val extras = FragmentNavigatorExtras(posterView to "poster_$movieId")
                findNavController().navigate(action, extras)
            }
        }

        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        // LoadStateAdapter(footer)가 전체 너비를 차지하도록 span 조정
        // 영화 아이템은 1칸(= 2열 그리드), footer는 2칸(= 전체 너비)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position < movieAdapter.itemCount) 1 else 2
            }
        }.apply { isSpanIndexCacheEnabled = true }

        binding.rvMovies.apply {
            layoutManager = gridLayoutManager
            adapter = movieAdapter.withLoadStateFooter(
                footer = MovieLoadStateAdapter { movieAdapter.retry() }
            )
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_now_playing))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_popular))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                collectMovies()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun observeLoadStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                movieAdapter.loadStateFlow.collectLatest { loadStates ->
                    val refreshState = loadStates.refresh

                    binding.shimmerView.shimmerLayout.isVisible = refreshState is LoadState.Loading
                    binding.rvMovies.isVisible = refreshState is LoadState.NotLoading
                    binding.errorView.layoutError.isVisible = refreshState is LoadState.Error

                    if (refreshState is LoadState.Loading) {
                        binding.shimmerView.shimmerLayout.startShimmer()
                    } else {
                        binding.shimmerView.shimmerLayout.stopShimmer()
                    }

                    if (refreshState is LoadState.Error) {
                        binding.errorView.tvErrorMessage.text =
                            ErrorMessageProvider.getErrorMessage(requireContext(), refreshState.error)
                        binding.errorView.btnRetry.setOnClickListener {
                            movieAdapter.retry()
                        }
                    }
                }
            }
        }
    }

    // 탭 전환 시 이전 수집 코루틴을 취소하고 새로 시작하여 코루틴 스태킹 방지
    private fun collectMovies() {
        collectJob?.cancel()
        collectJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val flow = if (currentTab == 0) {
                    viewModel.nowPlayingMovies
                } else {
                    viewModel.popularMovies
                }
                flow.collectLatest { pagingData ->
                    movieAdapter.submitData(pagingData)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        collectJob = null
        binding.rvMovies.adapter = null
        _binding = null
    }
}
