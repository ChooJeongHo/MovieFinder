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
import androidx.recyclerview.widget.LinearLayoutManager
import com.choo.moviefinder.R
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.databinding.FragmentHomeBinding
import com.choo.moviefinder.presentation.adapter.MovieLoadStateAdapter
import com.choo.moviefinder.presentation.adapter.HorizontalMovieAdapter
import com.choo.moviefinder.presentation.adapter.MoviePagingAdapter
import com.choo.moviefinder.presentation.common.createMovieGridLayoutManager
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
    private lateinit var watchHistoryAdapter: HorizontalMovieAdapter
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
        savedInstanceState?.let { currentTab = it.getInt(KEY_CURRENT_TAB, 0) }
        setupRecyclerView()
        setupWatchHistory()
        setupTabs()
        observeLoadStates()
        observeWatchHistory()
        collectMovies()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_TAB, currentTab)
    }

    private fun setupRecyclerView() {
        movieAdapter = MoviePagingAdapter { movieId, posterView ->
            if (findNavController().currentDestination?.id == R.id.homeFragment) {
                val action = HomeFragmentDirections.actionHomeToDetail(movieId)
                val extras = FragmentNavigatorExtras(posterView to "poster_$movieId")
                findNavController().navigate(action, extras)
            }
        }

        binding.rvMovies.apply {
            layoutManager = createMovieGridLayoutManager(requireContext()) {
                movieAdapter.itemCount
            }
            adapter = movieAdapter.withLoadStateFooter(
                footer = MovieLoadStateAdapter { movieAdapter.retry() }
            )
        }
    }

    private fun setupWatchHistory() {
        watchHistoryAdapter = HorizontalMovieAdapter(transitionPrefix = "poster_history") { movieId ->
            if (findNavController().currentDestination?.id == R.id.homeFragment) {
                val action = HomeFragmentDirections.actionHomeToDetail(movieId)
                findNavController().navigate(action)
            }
        }
        binding.rvWatchHistory.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = watchHistoryAdapter
        }
    }

    private fun observeWatchHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.watchHistory.collect { history ->
                    binding.watchHistorySection.isVisible = history.isNotEmpty()
                    watchHistoryAdapter.submitList(history)
                }
            }
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_now_playing))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_popular))

        if (currentTab != 0) {
            binding.tabLayout.getTabAt(currentTab)?.select()
        }

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
        binding.shimmerView.shimmerLayout.stopShimmer()
        collectJob = null
        binding.rvMovies.adapter = null
        binding.rvWatchHistory.adapter = null
        _binding = null
    }

    companion object {
        private const val KEY_CURRENT_TAB = "current_tab"
    }
}
