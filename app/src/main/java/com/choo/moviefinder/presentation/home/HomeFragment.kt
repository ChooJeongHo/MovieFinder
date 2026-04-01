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

    private val retryRateLimiter = RateLimiter(2_000L)

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var movieAdapter: MoviePagingAdapter
    private lateinit var watchHistoryAdapter: HorizontalMovieAdapter
    private var currentTab = 0
    private var collectJob: Job? = null

    // 홈 화면 레이아웃을 인플레이트하고 바인딩 초기화
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    // 뷰 생성 후 RecyclerView, 탭, SwipeRefresh 등 UI 컴포넌트 초기화
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.let { currentTab = it.getInt(KEY_CURRENT_TAB, 0) }
        setupRecyclerView()
        setupWatchHistory()
        setupSwipeRefresh()
        setupScrollToTopFab()
        setupTabs()
        observeLoadStates()
        observeWatchHistory()
        collectMovies()
    }

    // 현재 선택된 탭 인덱스를 저장하여 화면 회전 시 복원
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_TAB, currentTab)
    }

    // 영화 목록 RecyclerView에 PagingAdapter와 GridLayoutManager 설정
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

    // 시청 기록 가로 스크롤 RecyclerView 설정
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

    // 당겨서 새로고침 색상 및 리스너 설정
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
            requireContext().getColor(R.color.colorPrimary)
        )
        binding.swipeRefresh.setOnRefreshListener {
            movieAdapter.refresh()
        }
    }

    // 시청 기록 Flow를 수집하여 섹션 표시 여부 및 어댑터 갱신
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

    // 스크롤 위치에 따라 맨 위로 FAB 표시/숨김 및 클릭 동작 설정
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

    // 현재 상영작/인기 영화/트렌딩 탭 생성 및 탭 전환 리스너 설정
    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_now_playing))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_popular))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_trending))

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

    // Paging LoadState를 수집하여 Shimmer, 에러 뷰, SwipeRefresh 상태 전환
    private fun observeLoadStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                movieAdapter.loadStateFlow.collectLatest { loadStates ->
                    val refreshState = loadStates.refresh

                    val hasItems = movieAdapter.itemCount > 0
                    val isLoading = refreshState is LoadState.Loading
                    val isError = refreshState is LoadState.Error
                    val isInitialLoad = isLoading && !hasItems

                    binding.shimmerView.shimmerLayout.isVisible = isInitialLoad
                    binding.rvMovies.isVisible = !isError || hasItems
                    binding.swipeRefresh.isRefreshing = isLoading && hasItems
                    binding.errorView.layoutError.isVisible = isError && !hasItems

                    if (isInitialLoad) {
                        binding.shimmerView.shimmerLayout.startShimmer()
                    } else {
                        binding.shimmerView.shimmerLayout.stopShimmer()
                    }

                    if (refreshState is LoadState.Error) {
                        binding.errorView.tvErrorMessage.text =
                            ErrorMessageProvider.getErrorMessage(requireContext(), refreshState.error)
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

    // 탭 전환 시 이전 수집 코루틴을 취소하고 새로 시작하여 코루틴 스태킹 방지
    private fun collectMovies() {
        collectJob?.cancel()
        collectJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val flow = when (currentTab) {
                    0 -> viewModel.nowPlayingMovies
                    1 -> viewModel.popularMovies
                    else -> viewModel.trendingMovies
                }
                flow.collectLatest { pagingData ->
                    movieAdapter.submitData(pagingData)
                }
            }
        }
    }

    // Shimmer 중지, 어댑터 해제 및 바인딩 null 처리로 메모리 누수 방지
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
