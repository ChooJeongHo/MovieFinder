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
import androidx.recyclerview.widget.RecyclerView
import com.choo.moviefinder.R
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.core.util.RateLimiter
import com.choo.moviefinder.core.util.computeWindowWidthSizeClass
import com.choo.moviefinder.core.util.toMovieGridSpanCount
import com.choo.moviefinder.databinding.FragmentHomeBinding
import com.choo.moviefinder.presentation.adapter.MovieLoadStateAdapter
import com.choo.moviefinder.presentation.adapter.HorizontalMovieAdapter
import com.choo.moviefinder.presentation.adapter.MoviePagingAdapter
import com.choo.moviefinder.presentation.common.createMovieGridLayoutManager
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val retryRateLimiter = RateLimiter()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var movieAdapter: MoviePagingAdapter
    private lateinit var watchHistoryAdapter: HorizontalMovieAdapter

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
        setupRecyclerView()
        setupWatchHistory()
        setupSwipeRefresh()
        setupScrollToTopFab()
        setupTabs()
        observeLoadStates()
        observeWatchHistory()
        observeSelectedTab()
        collectCurrentMovies()
    }

    private fun setupRecyclerView() {
        movieAdapter = MoviePagingAdapter { movieId, posterView ->
            if (findNavController().currentDestination?.id == R.id.homeFragment) {
                val action = HomeFragmentDirections.actionHomeToDetail(movieId)
                val extras = FragmentNavigatorExtras(posterView to "poster_$movieId")
                findNavController().navigate(action, extras)
            }
        }

        val spanCount = requireActivity().computeWindowWidthSizeClass().toMovieGridSpanCount()
        binding.rvMovies.apply {
            layoutManager = createMovieGridLayoutManager(requireContext(), spanCount) {
                movieAdapter.itemCount
            }
            setHasFixedSize(true)
            itemAnimator = null
            recycledViewPool.setMaxRecycledViews(MoviePagingAdapter.VIEW_TYPE_GRID, POOL_SIZE_GRID)
            recycledViewPool.setMaxRecycledViews(MoviePagingAdapter.VIEW_TYPE_LIST, POOL_SIZE_LIST)
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

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
            requireContext().getColor(R.color.colorPrimary)
        )
        binding.swipeRefresh.setOnRefreshListener {
            movieAdapter.refresh()
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

    private fun setupScrollToTopFab() {
        binding.rvMovies.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!recyclerView.canScrollVertically(-1)) {
                    binding.fabScrollTop.hide()
                } else {
                    binding.fabScrollTop.show()
                }
            }
        })

        binding.fabScrollTop.setOnClickListener {
            binding.rvMovies.scrollToPosition(0)
            binding.fabScrollTop.hide()
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_now_playing))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_popular))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_trending))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_upcoming))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.onTabSelected(HomeTab.entries[tab.position])
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {
                binding.rvMovies.scrollToPosition(0)
            }
        })
    }

    // ViewModel의 selectedTab을 관찰해 TabLayout UI와 동기화한다.
    // StateFlow는 동일 값을 재방출하지 않으므로 select() 호출 → onTabSelected → onTabSelected 루프가 발생하지 않는다.
    private fun observeSelectedTab() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedTab.collect { tab ->
                    val tabView = binding.tabLayout.getTabAt(tab.ordinal)
                    if (tabView?.isSelected == false) tabView.select()
                }
            }
        }
    }

    private fun collectCurrentMovies() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentMovies.collectLatest { pagingData ->
                    movieAdapter.submitData(pagingData)
                }
            }
        }
    }

    private fun observeLoadStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                movieAdapter.loadStateFlow.collectLatest { loadStates ->
                    val refreshState = loadStates.refresh

                    val hasItems = movieAdapter.itemCount > 0
                    val isLoading = refreshState is LoadState.Loading
                    val isError = refreshState is LoadState.Error
                    val isInitialLoad = isLoading && !hasItems
                    val isEmpty = refreshState is LoadState.NotLoading &&
                        loadStates.append.endOfPaginationReached && !hasItems

                    binding.shimmerView.shimmerLayout.isVisible = isInitialLoad
                    binding.rvMovies.isVisible = hasItems && !isError
                    binding.swipeRefresh.isRefreshing = isLoading && hasItems
                    binding.errorView.layoutError.isVisible = isError && !hasItems
                    binding.emptyView.layoutEmpty.isVisible = isEmpty

                    if (isInitialLoad) {
                        binding.shimmerView.shimmerLayout.startShimmer()
                    } else {
                        binding.shimmerView.shimmerLayout.stopShimmer()
                    }

                    if (isEmpty) {
                        binding.emptyView.tvEmptyTitle.setText(R.string.home_empty_title)
                        binding.emptyView.tvEmptyMessage.setText(R.string.home_empty_message)
                        binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_movie)
                    }

                    if (refreshState is LoadState.Error) {
                        binding.errorView.tvErrorMessage.text =
                            ErrorMessageProvider.getMessage(requireContext(), refreshState.error)
                        binding.errorView.btnRetry.setOnClickListener {
                            if (retryRateLimiter.tryAcquire()) {
                                movieAdapter.retry()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.shimmerView.shimmerLayout.stopShimmer()
        binding.rvMovies.adapter = null
        binding.rvWatchHistory.adapter = null
        _binding = null
    }

    companion object {
        // 탭 수(4) × 화면에 동시에 보이는 최대 아이템 수 기준 (그리드: 3열×3행=9 → 여유 포함 20, 리스트: 5행 → 여유 포함 10)
        private const val POOL_SIZE_GRID = 20
        private const val POOL_SIZE_LIST = 10
    }
}
