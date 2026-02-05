package com.choo.moviefinder.presentation.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.choo.moviefinder.R
import com.choo.moviefinder.databinding.FragmentSearchBinding
import com.choo.moviefinder.presentation.adapter.MovieLoadStateAdapter
import com.choo.moviefinder.presentation.adapter.MoviePagingAdapter
import com.choo.moviefinder.presentation.adapter.RecentSearchAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()

    private lateinit var searchAdapter: MoviePagingAdapter
    private lateinit var recentSearchAdapter: RecentSearchAdapter

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
        setupYearFilter()
        setupEmptyStates()
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

        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        // LoadStateAdapter(footer)가 전체 너비를 차지하도록 span 조정
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position < searchAdapter.itemCount) 1 else 2
            }
        }.apply { isSpanIndexCacheEnabled = true }

        binding.rvSearchResults.apply {
            layoutManager = gridLayoutManager
            adapter = searchAdapter.withLoadStateFooter(
                footer = MovieLoadStateAdapter { searchAdapter.retry() }
            )
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

    private fun showYearFilterDialog() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val items = arrayOf(getString(R.string.filter_year_all)) +
            (currentYear downTo 1950).map { it.toString() }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.filter_year))
            .setItems(items) { _, which ->
                val year = if (which == 0) null else items[which].toIntOrNull()
                viewModel.onYearSelected(year)
                updateYearChip(year)
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

    private fun setupEmptyStates() {
        binding.emptyInitial.ivEmptyIcon.setImageResource(R.drawable.ic_search)
        binding.emptyInitial.tvEmptyTitle.text = getString(R.string.search_initial_title)
        binding.emptyInitial.tvEmptyMessage.text = getString(R.string.search_initial_message)

        binding.emptyNoResults.ivEmptyIcon.setImageResource(R.drawable.ic_movie)
        binding.emptyNoResults.tvEmptyTitle.text = getString(R.string.search_empty_title)
        binding.emptyNoResults.tvEmptyMessage.text = getString(R.string.search_empty_message)
    }

    // 3개의 독립적인 Flow를 각각 별도 coroutine으로 수집
    // - recentSearches: 검색어가 비어있을 때 최근 검색어 목록 표시 여부 결정
    // - searchResults: ViewModel의 debounce된 검색 결과 PagingData 수신
    // - loadStateFlow: 검색 결과의 로딩/에러/빈 상태에 따라 화면 전환
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recentSearches.collect { searches ->
                    recentSearchAdapter.submitList(searches)
                    val query = binding.etSearch.text?.toString() ?: ""
                    if (query.isBlank() && searches.isNotEmpty()) {
                        showRecentSearches()
                    } else if (query.isBlank()) {
                        showInitialState()
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
                    // 검색어가 비어있으면 loadState 변화를 무시 (최근 검색어 화면 유지)
                    val query = viewModel.searchQuery.value
                    if (query.isBlank()) return@collectLatest

                    val refreshState = loadStates.refresh

                    binding.shimmerView.shimmerLayout.isVisible = refreshState is LoadState.Loading
                    binding.rvSearchResults.isVisible = refreshState is LoadState.NotLoading
                    binding.recentSearchesSection.isVisible = false
                    binding.emptyInitial.layoutEmpty.isVisible = false

                    if (refreshState is LoadState.Loading) {
                        binding.shimmerView.shimmerLayout.startShimmer()
                        binding.emptyNoResults.layoutEmpty.isVisible = false
                    } else {
                        binding.shimmerView.shimmerLayout.stopShimmer()
                    }

                    if (refreshState is LoadState.NotLoading) {
                        val isEmpty = searchAdapter.itemCount == 0
                        binding.emptyNoResults.layoutEmpty.isVisible = isEmpty
                        binding.rvSearchResults.isVisible = !isEmpty
                    }
                }
            }
        }
    }

    // 검색어 입력 변경 시 즉시 호출되어 화면 상태를 전환
    // 검색어가 비면 결과/shimmer를 숨기고 최근검색어 또는 초기 안내 화면을 표시
    private fun updateVisibility(query: String) {
        if (query.isBlank()) {
            binding.rvSearchResults.isVisible = false
            binding.shimmerView.shimmerLayout.isVisible = false
            binding.emptyNoResults.layoutEmpty.isVisible = false

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
        binding.emptyNoResults.layoutEmpty.isVisible = false
    }

    private fun showInitialState() {
        binding.recentSearchesSection.isVisible = false
        binding.emptyInitial.layoutEmpty.isVisible = true
        binding.rvSearchResults.isVisible = false
        binding.emptyNoResults.layoutEmpty.isVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvSearchResults.adapter = null
        binding.rvRecentSearches.adapter = null
        _binding = null
    }
}
