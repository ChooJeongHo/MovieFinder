package com.choo.moviefinder.presentation.favorite

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import com.choo.moviefinder.core.util.computeWindowWidthSizeClass
import com.choo.moviefinder.core.util.toMovieGridSpanCount
import com.choo.moviefinder.databinding.FragmentFavoriteBinding
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.MovieTag
import com.choo.moviefinder.presentation.adapter.MovieAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentTab = savedInstanceState?.getInt(KEY_CURRENT_TAB, TAB_FAVORITES) ?: TAB_FAVORITES
        setupToolbar()
        setupRecyclerView()
        setupSwipeToDelete()
        setupTabs()
        updateSortLabel(viewModel.sortOrder.value)
        collectCurrentTab()
        observeViewModelFlows()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_TAB, currentTab)
    }

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

    private fun showSortDialog() {
        val sortOptions = FavoriteSortOrder.entries.toTypedArray()
        val sortLabels = sortOptions.map { getString(it.labelRes()) }.toTypedArray()
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

    private fun setupRecyclerView() {
        movieAdapter = MovieAdapter(
            onMovieClick = { movieId, posterView ->
                if (findNavController().currentDestination?.id == R.id.favoriteFragment) {
                    val action = FavoriteFragmentDirections.actionFavoriteToDetail(movieId)
                    val extras = FragmentNavigatorExtras(posterView to "poster_$movieId")
                    findNavController().navigate(action, extras)
                }
            },
            onMovieLongClick = { movie ->
                // 즐겨찾기 탭에서만 태그 관리 다이얼로그 표시
                if (currentTab == TAB_FAVORITES) {
                    showTagManagementDialog(movie)
                }
            }
        )

        binding.rvFavorites.apply {
            layoutManager = GridLayoutManager(
                requireContext(),
                requireActivity().computeWindowWidthSizeClass().toMovieGridSpanCount()
            )
            setHasFixedSize(true)
            adapter = movieAdapter
        }
    }

    private fun setupSwipeToDelete() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

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

    private fun onSwipeDelete(movie: Movie) {
        if (currentTab == TAB_FAVORITES) {
            viewModel.toggleFavorite(movie)
            Snackbar.make(binding.root, getString(R.string.favorite_removed), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.undo)) { viewModel.toggleFavorite(movie) }
                .show()
        } else {
            viewModel.toggleWatchlist(movie)
            Snackbar.make(binding.root, getString(R.string.watchlist_removed), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.undo)) { viewModel.toggleWatchlist(movie) }
                .show()
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.favorite_title))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.watchlist_title))

        if (currentTab != TAB_FAVORITES) {
            binding.tabLayout.getTabAt(currentTab)?.select()
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                // 워치리스트 탭 전환 시 태그 필터 초기화
                if (currentTab == TAB_WATCHLIST) viewModel.onTagSelected(null)
                updateTagFilterVisibility()
                collectCurrentTab()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {
                binding.rvFavorites.scrollToPosition(0)
            }
        })
    }

    // 태그/선택태그/스낵바 Flow를 단일 repeatOnLifecycle 블록에서 병렬 수집
    private fun observeViewModelFlows() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allTagNames.collect { tagNames ->
                        updateTagChips(tagNames)
                        updateTagFilterVisibility()
                    }
                }
                launch {
                    viewModel.selectedTag.collect { selectedTag ->
                        syncChipSelection(selectedTag)
                    }
                }
                launch {
                    viewModel.snackbarEvent.collect { errorType ->
                        val message = ErrorMessageProvider.getMessage(requireContext(), errorType)
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // "전체" + 태그 칩 동적 생성
    private fun updateTagChips(tagNames: List<String>) {
        val chipGroup = binding.chipGroupTags
        chipGroup.removeAllViews()

        val allChip = Chip(requireContext()).apply {
            text = getString(R.string.tag_filter_all)
            isCheckable = true
            isChecked = viewModel.selectedTag.value == null
            setOnClickListener { viewModel.onTagSelected(null) }
        }
        chipGroup.addView(allChip)

        tagNames.forEach { tagName ->
            val chip = Chip(requireContext()).apply {
                text = tagName
                isCheckable = true
                isChecked = viewModel.selectedTag.value == tagName
                setOnClickListener { viewModel.onTagSelected(tagName) }
            }
            chipGroup.addView(chip)
        }
    }

    // selectedTag에 따라 칩 선택 상태 동기화
    private fun syncChipSelection(selectedTag: String?) {
        val chipGroup = binding.chipGroupTags
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            chip.isChecked = if (i == 0) selectedTag == null else chip.text == selectedTag
        }
    }

    // 즐겨찾기 탭이고 태그가 존재할 때만 필터 표시
    private fun updateTagFilterVisibility() {
        binding.tagFilterScroll.isVisible =
            currentTab == TAB_FAVORITES && viewModel.allTagNames.value.isNotEmpty()
    }

    private fun collectCurrentTab() {
        collectJob?.cancel()
        movieAdapter.submitList(emptyList())
        binding.rvFavorites.scrollToPosition(0)
        // Hide both views to neutral state; first Room emission will set correct visibility
        binding.rvFavorites.isVisible = false
        binding.emptyView.layoutEmpty.isVisible = false
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

    // 태그 관리 다이얼로그: 기존 태그 삭제 + ML Kit 추천 태그 + 새 태그 추가
    private fun showTagManagementDialog(movie: Movie) {
        // MaterialAlertDialogBuilder의 테마 컨텍스트로 inflate해야 ?attr 속성이 정상 해석됨
        val dialogContext = MaterialAlertDialogBuilder(requireContext()).context
        val dialogView = LayoutInflater.from(dialogContext)
            .inflate(R.layout.dialog_manage_tags, null)
        val chipGroupExisting = dialogView.findViewById<ChipGroup>(R.id.chip_group_existing_tags)
        val chipGroupSuggested = dialogView.findViewById<ChipGroup>(R.id.chip_group_suggested_tags)
        val pbSuggestions = dialogView.findViewById<View>(R.id.pb_suggestions)
        val tvNoSuggestions = dialogView.findViewById<View>(R.id.tv_no_suggestions)
        val etNewTag = dialogView.findViewById<TextInputEditText>(R.id.et_new_tag)

        fun renderExistingTags(tags: List<MovieTag>) {
            chipGroupExisting.removeAllViews()
            dialogView.findViewById<View>(R.id.tv_existing_tags_label).isVisible = tags.isNotEmpty()
            tags.forEach { tag ->
                val chip = Chip(requireContext()).apply {
                    text = tag.tagName
                    isCloseIconVisible = true
                    setOnCloseIconClickListener {
                        viewModel.removeTagFromMovie(movie.id, tag.tagName)
                    }
                }
                chipGroupExisting.addView(chip)
            }
        }

        fun renderSuggestedTags(suggestions: List<String>) {
            pbSuggestions.isVisible = false
            chipGroupSuggested.removeAllViews()
            if (suggestions.isEmpty()) {
                tvNoSuggestions.isVisible = true
            } else {
                tvNoSuggestions.isVisible = false
                suggestions.forEach { suggestion ->
                    val chip = Chip(requireContext()).apply {
                        text = suggestion
                        // 추천 태그 칩 클릭 시 입력창에 자동 채움
                        setOnClickListener {
                            etNewTag.setText(suggestion)
                            etNewTag.setSelection(suggestion.length)
                        }
                    }
                    chipGroupSuggested.addView(chip)
                }
            }
        }

        activeDialog?.dismiss()
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(movie.title)
            .setView(dialogView)
            .setPositiveButton(R.string.tag_dialog_add) { _, _ ->
                val newTag = etNewTag.text?.toString()?.trim().orEmpty()
                if (newTag.isNotBlank()) {
                    viewModel.addTagToMovie(movie.id, newTag)
                }
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken, 0)
            }
            .setNegativeButton(R.string.action_close, null)
            .show()
        activeDialog = dialog

        // 기존 태그 실시간 관찰 (다이얼로그 닫힐 때 Job 취소하여 Room 쿼리 누수 방지)
        val tagJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getTagsForMovie(movie.id).collect { tags ->
                if (dialog.isShowing) renderExistingTags(tags)
            }
        }

        // ML Kit 포스터 분석 → 추천 태그 로드 (Job 추적하여 다이얼로그 닫힐 때 취소)
        val mlKitJob = viewLifecycleOwner.lifecycleScope.launch {
            pbSuggestions.isVisible = true
            val suggestions = viewModel.suggestTagsForPoster(movie.posterPath)
            if (dialog.isShowing) renderSuggestedTags(suggestions)
        }

        dialog.setOnDismissListener {
            tagJob.cancel()
            mlKitJob.cancel()
        }
    }

    private fun updateSortLabel(sort: FavoriteSortOrder) {
        binding.toolbar.subtitle = sort.subtitleRes()?.let { getString(it) }
    }

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

    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null
        swipeHelper?.attachToRecyclerView(null)
        swipeHelper = null
        collectJob?.cancel()
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

// FavoriteSortOrder → string resource 매핑 (도메인 레이어 순수성 유지)
private fun FavoriteSortOrder.labelRes(): Int = when (this) {
    FavoriteSortOrder.ADDED_DATE -> R.string.sort_added_date
    FavoriteSortOrder.TITLE -> R.string.sort_by_title
    FavoriteSortOrder.RATING -> R.string.sort_by_rating
}

private fun FavoriteSortOrder.subtitleRes(): Int? = when (this) {
    FavoriteSortOrder.ADDED_DATE -> null
    FavoriteSortOrder.TITLE -> R.string.sort_by_title
    FavoriteSortOrder.RATING -> R.string.sort_by_rating
}
