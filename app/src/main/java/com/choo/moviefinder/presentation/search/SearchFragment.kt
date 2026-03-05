package com.choo.moviefinder.presentation.search

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
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
import com.choo.moviefinder.databinding.FragmentSearchBinding
import com.choo.moviefinder.presentation.adapter.MovieLoadStateAdapter
import com.choo.moviefinder.presentation.adapter.MoviePagingAdapter
import com.choo.moviefinder.presentation.adapter.RecentSearchAdapter
import com.choo.moviefinder.presentation.common.createMovieGridLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

@Suppress("TooManyFunctions")
@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()

    private lateinit var searchAdapter: MoviePagingAdapter
    private lateinit var recentSearchAdapter: RecentSearchAdapter
    private var activeDialog: Dialog? = null

    private val yearFilterItems: Array<String> by lazy {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        arrayOf(getString(R.string.filter_year_all)) +
            (currentYear downTo 1950).map { it.toString() }.toTypedArray()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearchInput()
        setupRecyclerViews()
        setupScrollToTopFab()
        setupYearFilter()
        setupGenreFilter()
        setupSortFilter()
        setupEmptyStates()
        setupSuggestionChips()
        observeGenres()
        observeData()
    }

    private fun setupSearchInput() {
        binding.etSearch.doAfterTextChanged { text ->
            viewModel.onSearchQueryChange(text?.toString() ?: "")
            updateVisibility(text?.toString() ?: "")
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text?.toString() ?: ""
                if (query.isNotBlank()) {
                    viewModel.onSearch(query)
                }
                true
            } else {
                false
            }
        }
    }

    private fun setupRecyclerViews() {
        searchAdapter = MoviePagingAdapter { movieId, posterView ->
            if (findNavController().currentDestination?.id == R.id.searchFragment) {
                val action = SearchFragmentDirections.actionSearchToDetail(movieId)
                val extras = FragmentNavigatorExtras(posterView to "poster_$movieId")
                findNavController().navigate(action, extras)
            }
        }

        binding.rvSearchResults.apply {
            layoutManager = createMovieGridLayoutManager(requireContext()) {
                searchAdapter.itemCount
            }
            adapter = searchAdapter.withLoadStateFooter(
                footer = MovieLoadStateAdapter { searchAdapter.retry() }
            )
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(
                    recyclerView: RecyclerView,
                    newState: Int
                ) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        hideKeyboard()
                    }
                }
            })
        }

        recentSearchAdapter = RecentSearchAdapter(
            onItemClick = { query ->
                binding.etSearch.setText(query)
                binding.etSearch.setSelection(query.length)
                viewModel.onSearchQueryChange(query)
                viewModel.onSearch(query)
            },
            onDeleteClick = { query ->
                viewModel.onDeleteRecentSearch(query)
            }
        )

        binding.rvRecentSearches.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentSearchAdapter
        }

        binding.btnClearAll.setOnClickListener {
            viewModel.onClearSearchHistory()
        }
    }

    private fun setupYearFilter() {
        updateYearChip(viewModel.selectedYear.value)
        binding.chipYear.setOnClickListener { showYearFilterDialog() }
        binding.chipYear.setOnCloseIconClickListener {
            viewModel.onYearSelected(null)
            updateYearChip(null)
        }
    }

    private fun setupGenreFilter() {
        updateGenreChip(viewModel.selectedGenres.value)
        binding.chipGenre.setOnClickListener { showGenreFilterDialog() }
        binding.chipGenre.setOnCloseIconClickListener {
            viewModel.onGenresSelected(emptySet())
            updateGenreChip(emptySet())
            updateDiscoverModeChip()
        }
    }

    private fun setupSortFilter() {
        updateSortChip(viewModel.sortBy.value)
        binding.chipSort.setOnClickListener { showSortDialog() }
        binding.chipSort.setOnCloseIconClickListener {
            viewModel.onSortSelected(SortOption.POPULARITY_DESC)
            updateSortChip(SortOption.POPULARITY_DESC)
        }
    }

    private fun showYearFilterDialog() {
        val selectedYear = viewModel.selectedYear.value
        val checkedIndex = if (selectedYear == null) {
            0
        } else {
            yearFilterItems.indexOf(selectedYear.toString()).coerceAtLeast(0)
        }

        activeDialog?.dismiss()
        activeDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.filter_year))
            .setSingleChoiceItems(yearFilterItems, checkedIndex) { dialog, which ->
                val year = if (which == 0) null else yearFilterItems[which].toIntOrNull()
                viewModel.onYearSelected(year)
                updateYearChip(year)
                dialog.dismiss()
            }
            .show()
    }

    private fun showGenreFilterDialog() {
        val allGenres = viewModel.genres.value
        if (allGenres.isEmpty()) {
            viewModel.retryLoadGenres()
            Snackbar.make(
                binding.root,
                R.string.genre_load_failed,
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        val genreNames = allGenres.map { it.name }.toTypedArray()
        val selectedIds = viewModel.selectedGenres.value
        val checkedItems = BooleanArray(allGenres.size) { allGenres[it].id in selectedIds }

        activeDialog?.dismiss()
        activeDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.genre_select_title))
            .setMultiChoiceItems(genreNames, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton(getString(R.string.genre_confirm)) { _, _ ->
                val selected = allGenres.filterIndexed { index, _ -> checkedItems[index] }
                    .map { it.id }
                    .toSet()
                viewModel.onGenresSelected(selected)
                updateGenreChip(selected)
                updateDiscoverModeChip()
                // 검색어 없이 장르만으로 탐색
                if (viewModel.searchQuery.value.isBlank() && selected.isNotEmpty()) {
                    viewModel.onDiscoverWithFilters()
                }
            }
            .show()
    }

    private fun showSortDialog() {
        val sortOptions = SortOption.entries.toTypedArray()
        val sortLabels = arrayOf(
            getString(R.string.sort_popularity),
            getString(R.string.sort_vote_average),
            getString(R.string.sort_release_date),
            getString(R.string.sort_revenue)
        )
        val currentIndex = sortOptions.indexOf(viewModel.sortBy.value)

        activeDialog?.dismiss()
        activeDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.filter_sort))
            .setSingleChoiceItems(sortLabels, currentIndex) { dialog, which ->
                viewModel.onSortSelected(sortOptions[which])
                updateSortChip(sortOptions[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun updateYearChip(year: Int?) {
        if (year != null) {
            binding.chipYear.text = getString(R.string.filter_year_value, year)
            binding.chipYear.isCloseIconVisible = true
        } else {
            binding.chipYear.text = getString(R.string.filter_year)
            binding.chipYear.isCloseIconVisible = false
        }
    }

    private fun updateGenreChip(selectedGenreIds: Set<Int>) {
        if (selectedGenreIds.isNotEmpty()) {
            val allGenres = viewModel.genres.value
            val selectedNames = allGenres.filter { it.id in selectedGenreIds }
            val chipText = if (selectedNames.isEmpty()) {
                // 장르 목록 미로드 시 개수만 표시
                getString(R.string.genre_count, selectedGenreIds.size)
            } else if (selectedNames.size >= 3) {
                getString(R.string.genre_count, selectedNames.size)
            } else {
                selectedNames.joinToString(", ") { it.name }
            }
            binding.chipGenre.text = chipText
            binding.chipGenre.isCloseIconVisible = true
        } else {
            binding.chipGenre.text = getString(R.string.filter_genre)
            binding.chipGenre.isCloseIconVisible = false
        }
    }

    private fun updateSortChip(sort: SortOption) {
        if (sort != SortOption.POPULARITY_DESC) {
            val label = when (sort) {
                SortOption.POPULARITY_DESC -> getString(R.string.sort_popularity)
                SortOption.VOTE_AVERAGE_DESC -> getString(R.string.sort_vote_average)
                SortOption.RELEASE_DATE_DESC -> getString(R.string.sort_release_date)
                SortOption.REVENUE_DESC -> getString(R.string.sort_revenue)
            }
            binding.chipSort.text = label
            binding.chipSort.isCloseIconVisible = true
        } else {
            binding.chipSort.text = getString(R.string.filter_sort)
            binding.chipSort.isCloseIconVisible = false
        }
    }

    private fun setupScrollToTopFab() {
        binding.rvSearchResults.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!recyclerView.canScrollVertically(-1)) {
                    binding.fabScrollTop.hide()
                } else {
                    binding.fabScrollTop.show()
                }
            }
        })

        binding.fabScrollTop.setOnClickListener {
            binding.rvSearchResults.scrollToPosition(0)
            binding.fabScrollTop.hide()
        }
    }

    private fun setupEmptyStates() {
        binding.emptyInitial.ivEmptyIcon.setImageResource(R.drawable.ic_search)
        binding.emptyInitial.tvEmptyTitle.text = getString(R.string.search_initial_title)
        binding.emptyInitial.tvEmptyMessage.text = getString(R.string.search_initial_message)

        binding.emptyNoResults.ivEmptyIcon.setImageResource(R.drawable.ic_movie)
        binding.emptyNoResults.tvEmptyTitle.text = getString(R.string.search_empty_title)
        binding.emptyNoResults.tvEmptyMessage.text = getString(R.string.search_empty_message)
    }

    private fun setupSuggestionChips() {
        val suggestions = listOf("마블", "스파이더맨", "배트맨", "스타워즈", "해리포터")
        binding.chipGroupSuggestions.removeAllViews()
        for (term in suggestions) {
            val chip = Chip(requireContext()).apply {
                text = term
                isClickable = true
                isCheckable = false
            }
            chip.setOnClickListener {
                binding.etSearch.setText(term)
                binding.etSearch.setSelection(term.length)
                viewModel.onSearchQueryChange(term)
                viewModel.onSearch(term)
            }
            binding.chipGroupSuggestions.addView(chip)
        }
    }

    // 4개의 독립적인 Flow를 각각 별도 coroutine으로 수집
    // - genres: 장르 목록 로드 완료 시 칩 텍스트 갱신 (프로세스 복원 대응)
    // - recentSearches: 검색어가 비어있을 때 최근 검색어 목록 표시 여부 결정
    // - searchResults: ViewModel의 debounce된 검색 결과 PagingData 수신
    // - loadStateFlow: 검색 결과의 로딩/에러/빈 상태에 따라 화면 전환
    private fun observeGenres() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.genres.collect { genres ->
                    if (genres.isNotEmpty()) {
                        updateGenreChip(viewModel.selectedGenres.value)
                    }
                }
            }
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recentSearches.collect { searches ->
                    val query = binding.etSearch.text?.toString()?.trim() ?: ""
                    if (query.isBlank()) {
                        recentSearchAdapter.submitList(searches)
                        if (viewModel.selectedGenres.value.isEmpty() && searches.isNotEmpty()) {
                            showRecentSearches()
                        } else if (viewModel.selectedGenres.value.isEmpty()) {
                            showInitialState()
                        }
                    } else {
                        // 입력 중 자동완성: 최근 검색어 중 매칭되는 항목 필터링
                        val filtered = searches.filter {
                            it.contains(query, ignoreCase = true) && it != query
                        }
                        recentSearchAdapter.submitList(filtered)
                        if (filtered.isNotEmpty()) {
                            binding.recentSearchesSection.isVisible = true
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchResults.collectLatest { pagingData ->
                    searchAdapter.submitData(pagingData)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                searchAdapter.loadStateFlow.collectLatest { loadStates ->
                    // 검색어가 비어있고 장르 필터도 없으면 loadState 변화를 무시 (최근 검색어 화면 유지)
                    val query = viewModel.searchQuery.value
                    val hasGenreFilter = viewModel.selectedGenres.value.isNotEmpty()
                    if (query.isBlank() && !hasGenreFilter) return@collectLatest

                    val refreshState = loadStates.refresh

                    binding.shimmerView.shimmerLayout.isVisible = refreshState is LoadState.Loading
                    binding.rvSearchResults.isVisible = refreshState is LoadState.NotLoading
                    binding.recentSearchesSection.isVisible = false
                    binding.emptyInitial.layoutEmpty.isVisible = false

                    if (refreshState is LoadState.Loading) {
                        binding.shimmerView.shimmerLayout.startShimmer()
                        binding.noResultsSection.isVisible = false
                    } else {
                        binding.shimmerView.shimmerLayout.stopShimmer()
                    }

                    if (refreshState is LoadState.NotLoading) {
                        val isEmpty = searchAdapter.itemCount == 0
                        binding.noResultsSection.isVisible = isEmpty
                        binding.rvSearchResults.isVisible = !isEmpty
                    }
                }
            }
        }
    }

    private fun updateDiscoverModeChip() {
        val query = viewModel.searchQuery.value
        val hasGenres = viewModel.selectedGenres.value.isNotEmpty()
        binding.chipDiscoverMode.isVisible = query.isBlank() && hasGenres
    }

    // 검색어 입력 변경 시 즉시 호출되어 화면 상태를 전환
    // 검색어가 비면 결과/shimmer를 숨기고 최근검색어 또는 초기 안내 화면을 표시
    private fun updateVisibility(query: String) {
        updateDiscoverModeChip()
        if (query.isBlank() && viewModel.selectedGenres.value.isEmpty()) {
            binding.rvSearchResults.isVisible = false
            binding.shimmerView.shimmerLayout.isVisible = false
            binding.noResultsSection.isVisible = false

            val searches = viewModel.recentSearches.value
            if (searches.isNotEmpty()) {
                showRecentSearches()
            } else {
                showInitialState()
            }
        }
    }

    private fun showRecentSearches() {
        binding.recentSearchesSection.isVisible = true
        binding.emptyInitial.layoutEmpty.isVisible = false
        binding.rvSearchResults.isVisible = false
        binding.noResultsSection.isVisible = false
        binding.fabScrollTop.hide()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        binding.etSearch.clearFocus()
    }

    private fun showInitialState() {
        binding.recentSearchesSection.isVisible = false
        binding.emptyInitial.layoutEmpty.isVisible = true
        binding.rvSearchResults.isVisible = false
        binding.noResultsSection.isVisible = false
        binding.fabScrollTop.hide()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null
        binding.shimmerView.shimmerLayout.stopShimmer()
        binding.rvSearchResults.adapter = null
        binding.rvRecentSearches.adapter = null
        _binding = null
    }
}
