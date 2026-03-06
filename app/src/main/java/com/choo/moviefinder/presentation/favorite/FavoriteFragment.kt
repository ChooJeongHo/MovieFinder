package com.choo.moviefinder.presentation.favorite

import android.app.Dialog
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.choo.moviefinder.R
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.databinding.FragmentFavoriteBinding
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.presentation.adapter.MovieAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoriteFragment : Fragment() {

    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoriteViewModel by viewModels()

    private lateinit var movieAdapter: MovieAdapter
    private var currentTab = TAB_FAVORITES
    private var collectJob: Job? = null
    private var swipeHelper: ItemTouchHelper? = null
    private var activeDialog: Dialog? = null

    // 즐겨찾기 화면 레이아웃을 인플레이트하고 바인딩 초기화
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    // 뷰 생성 후 툴바, RecyclerView, 스와이프 삭제, 탭 등 UI 초기화
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.let { currentTab = it.getInt(KEY_CURRENT_TAB, TAB_FAVORITES) }
        setupToolbar()
        setupRecyclerView()
        setupSwipeToDelete()
        setupTabs()
        updateSortLabel(viewModel.sortOrder.value)
        collectCurrentTab()
        observeSnackbar()
    }

    // 현재 선택된 탭 인덱스를 저장하여 화면 회전 시 복원
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_TAB, currentTab)
    }

    // 툴바에 정렬 메뉴 설정
    private fun setupToolbar() {
        binding.toolbar.inflateMenu(R.menu.menu_favorite)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_sort -> {
                    showSortDialog()
                    true
                }
                else -> false
            }
        }
    }

    // 정렬 옵션 단일 선택 다이얼로그 표시
    private fun showSortDialog() {
        val sortOptions = FavoriteSortOrder.entries.toTypedArray()
        val sortLabels = arrayOf(
            getString(R.string.sort_added_date),
            getString(R.string.sort_by_title),
            getString(R.string.sort_by_rating)
        )
        val currentIndex = sortOptions.indexOf(viewModel.sortOrder.value)

        activeDialog?.dismiss()
        activeDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.sort_title))
            .setSingleChoiceItems(sortLabels, currentIndex) { dialog, which ->
                viewModel.onSortOrderSelected(sortOptions[which])
                updateSortLabel(sortOptions[which])
                dialog.dismiss()
            }
            .show()
    }

    // 영화 목록 RecyclerView에 GridLayoutManager와 MovieAdapter 설정
    private fun setupRecyclerView() {
        movieAdapter = MovieAdapter { movieId, posterView ->
            if (findNavController().currentDestination?.id == R.id.favoriteFragment) {
                val action = FavoriteFragmentDirections.actionFavoriteToDetail(movieId)
                val extras = FragmentNavigatorExtras(posterView to "poster_$movieId")
                findNavController().navigate(action, extras)
            }
        }

        binding.rvFavorites.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = movieAdapter
        }
    }

    // 왼쪽 스와이프 삭제 ItemTouchHelper 설정
    private fun setupSwipeToDelete() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val movie = movieAdapter.currentList.getOrNull(position) ?: return
                onSwipeDelete(movie)
            }
        }
        swipeHelper = ItemTouchHelper(callback)
        swipeHelper?.attachToRecyclerView(binding.rvFavorites)
    }

    // 스와이프 삭제 처리 및 Undo Snackbar 표시
    private fun onSwipeDelete(movie: Movie) {
        if (currentTab == TAB_FAVORITES) {
            viewModel.toggleFavorite(movie)
            Snackbar.make(binding.root, getString(R.string.favorite_removed), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.undo)) {
                    viewModel.toggleFavorite(movie)
                }
                .show()
        } else {
            viewModel.toggleWatchlist(movie)
            Snackbar.make(binding.root, getString(R.string.watchlist_removed), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.undo)) {
                    viewModel.toggleWatchlist(movie)
                }
                .show()
        }
    }

    // 즐겨찾기/워치리스트 탭 생성 및 탭 전환 리스너 설정
    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.favorite_title))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.watchlist_title))

        if (currentTab != TAB_FAVORITES) {
            binding.tabLayout.getTabAt(currentTab)?.select()
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                collectCurrentTab()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    // 현재 탭에 해당하는 영화 목록 Flow를 수집하여 UI 갱신
    private fun collectCurrentTab() {
        collectJob?.cancel()
        movieAdapter.submitList(emptyList())
        binding.rvFavorites.scrollToPosition(0)
        updateEmptyState()
        collectJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val flow = if (currentTab == TAB_FAVORITES) {
                    viewModel.favoriteMovies
                } else {
                    viewModel.watchlistMovies
                }
                flow.collect { movies ->
                    movieAdapter.submitList(movies)
                    binding.rvFavorites.isVisible = movies.isNotEmpty()
                    binding.emptyView.layoutEmpty.isVisible = movies.isEmpty()
                }
            }
        }
    }

    // 에러 이벤트를 수집하여 Snackbar로 표시
    private fun observeSnackbar() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.snackbarEvent.collect { errorType ->
                    val message = ErrorMessageProvider.getMessage(requireContext(), errorType)
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 현재 정렬 옵션을 툴바 subtitle로 표시
    private fun updateSortLabel(sort: FavoriteSortOrder) {
        val subtitle = when (sort) {
            FavoriteSortOrder.ADDED_DATE -> null
            FavoriteSortOrder.TITLE -> getString(R.string.sort_by_title)
            FavoriteSortOrder.RATING -> getString(R.string.sort_by_rating)
        }
        binding.toolbar.subtitle = subtitle
    }

    // 현재 탭에 맞는 빈 상태 아이콘 및 메시지 설정
    private fun updateEmptyState() {
        if (currentTab == TAB_FAVORITES) {
            binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_favorite_border)
            binding.emptyView.tvEmptyTitle.text = getString(R.string.favorite_empty_title)
            binding.emptyView.tvEmptyMessage.text = getString(R.string.favorite_empty_message)
        } else {
            binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_watchlist_border)
            binding.emptyView.tvEmptyTitle.text = getString(R.string.watchlist_empty_title)
            binding.emptyView.tvEmptyMessage.text = getString(R.string.watchlist_empty_message)
        }
    }

    // 다이얼로그 dismiss, SwipeHelper 해제, 어댑터 및 바인딩 null 처리
    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null
        swipeHelper?.attachToRecyclerView(null)
        swipeHelper = null
        collectJob = null
        binding.rvFavorites.adapter = null
        _binding = null
    }

    companion object {
        private const val KEY_CURRENT_TAB = "favorite_current_tab"
        private const val TAB_FAVORITES = 0
        private const val TAB_WATCHLIST = 1
    }
}
