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
import androidx.paging.CombinedLoadStates
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.domain.model.PersonSearchItem
import com.choo.moviefinder.core.util.computeWindowWidthSizeClass
import com.choo.moviefinder.core.util.toMovieGridSpanCount
import com.choo.moviefinder.databinding.FragmentSearchBinding
import com.choo.moviefinder.presentation.adapter.MovieLoadStateAdapter
import com.choo.moviefinder.presentation.adapter.MoviePagingAdapter
import com.choo.moviefinder.presentation.adapter.PersonSearchAdapter
import com.choo.moviefinder.presentation.adapter.RecentSearchAdapter
import com.choo.moviefinder.presentation.common.createMovieGridLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@Suppress("TooManyFunctions")
@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()

    private lateinit var searchAdapter: MoviePagingAdapter
    private lateinit var recentSearchAdapter: RecentSearchAdapter
    private lateinit var personSearchAdapter: PersonSearchAdapter
    private var activeDialog: Dialog? = null

    private val yearFilterItems: Array<String> by lazy {
        val currentYear = Clock.System.todayIn(TimeZone.currentSystemDefault()).year
        arrayOf(getString(R.string.filter_year_all)) +
            (currentYear downTo 1950).map { it.toString() }.toTypedArray()
    }

    // 검색 화면 레이아웃을 인플레이트하고 바인딩 초기화
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    // 뷰 생성 후 검색 입력, 필터, RecyclerView 등 전체 UI 초기화
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearchInput()
        setupRecyclerViews()
        setupScrollToTopFab()
        setupViewModeToggle()
        setupSearchModeToggle()
        setupYearFilter()
        setupGenreFilter()
        setupSortFilter()
        setupEmptyStates()
        setupSuggestionChips()
        observeViewModelFlows()
    }

    // 검색 입력 텍스트 변경 감지 및 키보드 검색 액션 리스너 설정
    private fun setupSearchInput() {
        binding.etSearch.doAfterTextChanged { text ->
            val query = text?.toString() ?: ""
            if (viewModel.searchMode.value == SearchMode.PERSON) {
                viewModel.onPersonSearch(query)
            } else {
                viewModel.onSearchQueryChange(query)
                updateVisibility(query)
            }
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text?.toString() ?: ""
                if (query.isNotBlank()) {
                    if (viewModel.searchMode.value == SearchMode.PERSON) {
                        viewModel.onPersonSearch(query)
                    } else {
                        viewModel.onSearch(query)
                    }
                }
                true
            } else {
                false
            }
        }
    }

    // 검색 결과 및 최근 검색어 RecyclerView 초기화
    private fun setupRecyclerViews() {
        searchAdapter = MoviePagingAdapter { movieId, posterView ->
            if (findNavController().currentDestination?.id == R.id.searchFragment) {
                val action = SearchFragmentDirections.actionSearchToDetail(movieId)
                val extras = FragmentNavigatorExtras(posterView to "poster_$movieId")
                findNavController().navigate(action, extras)
            }
        }

        val spanCount = requireActivity().computeWindowWidthSizeClass().toMovieGridSpanCount()
        binding.rvSearchResults.apply {
            layoutManager = createMovieGridLayoutManager(requireContext(), spanCount) {
                searchAdapter.itemCount
            }
            setHasFixedSize(true)
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
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.search_clear_history_confirm_title)
                .setMessage(R.string.search_clear_history_confirm_message)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    viewModel.onClearSearchHistory()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        personSearchAdapter = PersonSearchAdapter { personId ->
            if (findNavController().currentDestination?.id == R.id.searchFragment) {
                val action = SearchFragmentDirections.actionSearchToPersonDetail(personId)
                findNavController().navigate(action)
            }
        }

        binding.rvPersonResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = personSearchAdapter
        }
    }

    // 툴바 메뉴에 그리드/리스트 보기 모드 전환 버튼 설정
    private fun setupViewModeToggle() {
        updateViewModeIcon(viewModel.viewMode.value)
        binding.toolbar.inflateMenu(R.menu.menu_search)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_view_mode -> {
                    viewModel.toggleViewMode()
                    true
                }
                else -> false
            }
        }
    }

    private fun observeViewModelFlows() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.viewMode.collect { handleViewMode(it) } }
                launch { viewModel.searchMode.collect { handleSearchMode(it) } }
                launch { viewModel.personResults.collect { handlePersonResults(it) } }
                launch { viewModel.isPersonSearchLoading.collect { handlePersonLoading(it) } }
                launch {
                    viewModel.genres.collect { genres ->
                        if (genres.isNotEmpty()) updateGenreChip(viewModel.selectedGenres.value)
                    }
                }
                launch { viewModel.recentSearches.collect { handleRecentSearches(it) } }
                launch { viewModel.searchResults.collectLatest { searchAdapter.submitData(it) } }
                launch { searchAdapter.loadStateFlow.collectLatest { handleLoadStates(it) } }
                launch { viewModel.snackbarEvent.collect { showSearchSnackbar(it) } }
            }
        }
    }

    private fun handleViewMode(mode: MoviePagingAdapter.ViewMode) {
        searchAdapter.viewMode = mode
        updateViewModeIcon(mode)
        applyLayoutManager(mode)
    }

    private fun handleSearchMode(mode: SearchMode) {
        val isPersonMode = mode == SearchMode.PERSON
        binding.chipGroupFilters.isVisible = !isPersonMode
        binding.chipDiscoverMode.isVisible = false
        binding.rvPersonResults.isVisible = false
        binding.rvSearchResults.isVisible = false
        binding.noResultsSection.isVisible = false
        binding.shimmerView.shimmerLayout.isVisible = false
        if (isPersonMode) {
            binding.recentSearchesSection.isVisible = false
            binding.emptyInitial.layoutEmpty.isVisible = true
            binding.emptyInitial.ivEmptyIcon.setImageResource(R.drawable.ic_person)
            binding.emptyInitial.tvEmptyTitle.text = getString(R.string.search_person_initial_title)
            binding.emptyInitial.tvEmptyMessage.text = getString(R.string.search_person_initial_message)
            val query = binding.etSearch.text?.toString() ?: ""
            if (query.isNotBlank()) viewModel.onPersonSearch(query)
        } else {
            binding.emptyInitial.ivEmptyIcon.setImageResource(R.drawable.ic_search)
            binding.emptyInitial.tvEmptyTitle.text = getString(R.string.search_initial_title)
            binding.emptyInitial.tvEmptyMessage.text = getString(R.string.search_initial_message)
            updateVisibility(binding.etSearch.text?.toString() ?: "")
        }
    }

    private fun handlePersonResults(results: List<PersonSearchItem>) {
        if (viewModel.searchMode.value != SearchMode.PERSON) return
        personSearchAdapter.submitList(results)
        val query = binding.etSearch.text?.toString()?.trim() ?: ""
        when {
            query.isBlank() -> {
                binding.rvPersonResults.isVisible = false
                binding.emptyInitial.layoutEmpty.isVisible = true
                binding.noResultsSection.isVisible = false
            }
            results.isEmpty() && !viewModel.isPersonSearchLoading.value -> {
                binding.rvPersonResults.isVisible = false
                binding.emptyInitial.layoutEmpty.isVisible = false
                binding.noResultsSection.isVisible = true
            }
            else -> {
                binding.rvPersonResults.isVisible = results.isNotEmpty()
                binding.emptyInitial.layoutEmpty.isVisible = false
                binding.noResultsSection.isVisible = false
            }
        }
    }

    private fun handlePersonLoading(isLoading: Boolean) {
        if (viewModel.searchMode.value != SearchMode.PERSON) return
        binding.shimmerView.shimmerLayout.isVisible = isLoading
        if (isLoading) {
            binding.shimmerView.shimmerLayout.startShimmer()
            binding.rvPersonResults.isVisible = false
            binding.noResultsSection.isVisible = false
        } else {
            binding.shimmerView.shimmerLayout.stopShimmer()
        }
    }

    private fun handleRecentSearches(searches: List<String>) {
        val query = binding.etSearch.text?.toString()?.trim() ?: ""
        if (query.isBlank()) {
            recentSearchAdapter.submitList(searches)
            if (viewModel.selectedGenres.value.isEmpty() && searches.isNotEmpty()) {
                showRecentSearches()
            } else if (viewModel.selectedGenres.value.isEmpty()) {
                showInitialState()
            }
        } else {
            val filtered = searches.filter { it.contains(query, ignoreCase = true) && it != query }
            recentSearchAdapter.submitList(filtered)
            if (filtered.isNotEmpty()) binding.recentSearchesSection.isVisible = true
        }
    }

    private fun handleLoadStates(loadStates: CombinedLoadStates) {
        val query = viewModel.searchQuery.value
        val hasGenreFilter = viewModel.selectedGenres.value.isNotEmpty()
        if (query.isBlank() && !hasGenreFilter) return
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

    private fun showSearchSnackbar(errorType: ErrorType) {
        Snackbar.make(
            binding.root,
            ErrorMessageProvider.getMessage(requireContext(), errorType),
            Snackbar.LENGTH_SHORT
        ).show()
    }

    // 보기 모드에 따라 GridLayoutManager 또는 LinearLayoutManager 적용
    private fun applyLayoutManager(mode: MoviePagingAdapter.ViewMode) {
        binding.rvSearchResults.layoutManager = if (mode == MoviePagingAdapter.ViewMode.LIST) {
            LinearLayoutManager(requireContext())
        } else {
            createMovieGridLayoutManager(requireContext()) {
                searchAdapter.itemCount
            }
        }
    }

    // 현재 보기 모드에 따라 툴바 메뉴 아이콘 전환
    private fun updateViewModeIcon(mode: MoviePagingAdapter.ViewMode) {
        val icon = if (mode == MoviePagingAdapter.ViewMode.GRID) {
            R.drawable.ic_view_list
        } else {
            R.drawable.ic_view_grid
        }
        binding.toolbar.menu.findItem(R.id.action_view_mode)?.setIcon(icon)
    }

    // 연도 필터 칩 초기값 설정 및 클릭/닫기 리스너 등록
    private fun setupYearFilter() {
        updateYearChip(viewModel.selectedYear.value)
        binding.chipYear.setOnClickListener { showYearFilterDialog() }
        binding.chipYear.setOnCloseIconClickListener {
            viewModel.onYearSelected(null)
            updateYearChip(null)
        }
    }

    // 장르 필터 칩 초기값 설정 및 클릭/닫기 리스너 등록
    private fun setupGenreFilter() {
        updateGenreChip(viewModel.selectedGenres.value)
        binding.chipGenre.setOnClickListener { showGenreFilterDialog() }
        binding.chipGenre.setOnCloseIconClickListener {
            viewModel.onGenresSelected(emptySet())
            updateGenreChip(emptySet())
            updateDiscoverModeChip()
        }
    }

    // 정렬 필터 칩 초기값 설정 및 클릭/닫기 리스너 등록
    private fun setupSortFilter() {
        updateSortChip(viewModel.sortBy.value)
        binding.chipSort.setOnClickListener { showSortDialog() }
        binding.chipSort.setOnCloseIconClickListener {
            viewModel.onSortSelected(SortOption.POPULARITY_DESC)
            updateSortChip(SortOption.POPULARITY_DESC)
        }
    }

    // 연도 선택 다이얼로그 표시 (현재 선택 상태 반영)
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

    // 장르 다중 선택 다이얼로그 표시 (미로드 시 재시도 안내)
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

    // 정렬 옵션 단일 선택 다이얼로그 표시
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

    // 연도 칩 텍스트 및 닫기 아이콘 표시 갱신
    private fun updateYearChip(year: Int?) {
        if (year != null) {
            binding.chipYear.text = getString(R.string.filter_year_value, year)
            binding.chipYear.isCloseIconVisible = true
        } else {
            binding.chipYear.text = getString(R.string.filter_year)
            binding.chipYear.isCloseIconVisible = false
        }
    }

    // 장르 칩 텍스트 갱신 (3개 이상 시 개수 표시, 미로드 시 개수만 표시)
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

    // 정렬 칩 텍스트 및 닫기 아이콘 표시 갱신
    private fun updateSortChip(sort: SortOption) {
        if (sort != SortOption.POPULARITY_DESC) {
            val label = when (sort) {
                SortOption.VOTE_AVERAGE_DESC -> getString(R.string.sort_vote_average)
                SortOption.RELEASE_DATE_DESC -> getString(R.string.sort_release_date)
                SortOption.REVENUE_DESC -> getString(R.string.sort_revenue)
                SortOption.POPULARITY_DESC -> getString(R.string.sort_popularity)
            }
            binding.chipSort.text = label
            binding.chipSort.isCloseIconVisible = true
        } else {
            binding.chipSort.text = getString(R.string.filter_sort)
            binding.chipSort.isCloseIconVisible = false
        }
    }

    // 스크롤 위치에 따라 맨 위로 FAB 표시/숨김 및 클릭 동작 설정
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

    // 초기 상태 및 결과 없음 빈 화면의 아이콘과 텍스트 설정
    private fun setupEmptyStates() {
        binding.emptyInitial.ivEmptyIcon.setImageResource(R.drawable.ic_search)
        binding.emptyInitial.tvEmptyTitle.text = getString(R.string.search_initial_title)
        binding.emptyInitial.tvEmptyMessage.text = getString(R.string.search_initial_message)

        binding.emptyNoResults.ivEmptyIcon.setImageResource(R.drawable.ic_movie)
        binding.emptyNoResults.tvEmptyTitle.text = getString(R.string.search_empty_title)
        binding.emptyNoResults.tvEmptyMessage.text = getString(R.string.search_empty_message)
    }

    // 결과 없을 때 표시할 추천 검색어 칩 동적 생성
    private fun setupSuggestionChips() {
        val suggestions = resources.getStringArray(R.array.search_suggestions).toList()
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

    // 검색 모드 전환 칩 그룹 설정 (영화/배우)
    private fun setupSearchModeToggle() {
        binding.chipGroupSearchMode.setOnCheckedStateChangeListener { _, checkedIds ->
            val newMode = if (checkedIds.contains(R.id.chip_mode_person)) {
                SearchMode.PERSON
            } else {
                SearchMode.MOVIE
            }
            if (viewModel.searchMode.value != newMode) {
                viewModel.toggleSearchMode()
            }
        }
    }

    // 검색어 없이 장르 필터만 사용 중일 때 Discover 모드 칩 표시
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

    // 최근 검색어 섹션을 표시하고 다른 뷰를 숨김
    private fun showRecentSearches() {
        binding.recentSearchesSection.isVisible = true
        binding.emptyInitial.layoutEmpty.isVisible = false
        binding.rvSearchResults.isVisible = false
        binding.noResultsSection.isVisible = false
        binding.fabScrollTop.hide()
    }

    // 소프트 키보드를 숨기고 검색 입력 포커스 해제
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        binding.etSearch.clearFocus()
    }

    // 검색 초기 안내 화면을 표시하고 다른 뷰를 숨김
    private fun showInitialState() {
        binding.recentSearchesSection.isVisible = false
        binding.emptyInitial.layoutEmpty.isVisible = true
        binding.rvSearchResults.isVisible = false
        binding.noResultsSection.isVisible = false
        binding.fabScrollTop.hide()
    }

    // 다이얼로그 dismiss, Shimmer 중지, 어댑터 해제 및 바인딩 null 처리
    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null
        binding.shimmerView.shimmerLayout.stopShimmer()
        binding.rvSearchResults.adapter = null
        binding.rvRecentSearches.adapter = null
        binding.rvPersonResults.adapter = null
        _binding = null
    }
}
