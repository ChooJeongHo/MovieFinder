package com.choo.moviefinder.presentation.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.choo.moviefinder.R
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.databinding.FragmentStatsBinding
import com.choo.moviefinder.domain.model.WatchStats
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatsViewModel by viewModels()

    // 통계 화면 레이아웃을 인플레이트하고 바인딩 초기화
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // 뷰 생성 후 툴바, UI 상태 관찰 설정
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        observeUiState()
    }

    // 툴바 뒤로가기 버튼 설정
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    // UI 상태(Loading/Success/Error) Flow를 수집하여 화면 전환
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is StatsUiState.Loading -> showLoading()
                        is StatsUiState.Success -> showContent(state.stats)
                        is StatsUiState.Error -> showError(state.errorType)
                    }
                }
            }
        }
    }

    // 로딩 상태 표시
    private fun showLoading() {
        binding.progressBar.isVisible = true
        binding.contentLayout.isVisible = false
        binding.errorView.layoutError.isVisible = false
    }

    // 성공 상태 표시 (통계 데이터 바인딩)
    private fun showContent(stats: WatchStats) {
        binding.progressBar.isVisible = false
        binding.contentLayout.isVisible = true
        binding.errorView.layoutError.isVisible = false

        binding.tvTotalWatched.text = getString(R.string.stats_count_format, stats.totalWatched)
        binding.tvMonthlyWatched.text = getString(R.string.stats_count_format, stats.monthlyWatched)

        if (stats.averageRating != null) {
            binding.tvAverageRating.text = getString(R.string.stats_rating_format, stats.averageRating)
        } else {
            binding.tvAverageRating.text = getString(R.string.stats_no_rating)
        }

        binding.genreContainer.removeAllViews()
        if (stats.topGenres.isEmpty()) {
            binding.tvGenreEmpty.isVisible = true
        } else {
            binding.tvGenreEmpty.isVisible = false
            stats.topGenres.forEachIndexed { index, genreCount ->
                val textView = TextView(requireContext()).apply {
                    text = getString(R.string.stats_genre_item, index + 1, genreCount.name, genreCount.count)
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Body1)
                }
                binding.genreContainer.addView(textView)
            }
        }
    }

    // 에러 상태 표시 (에러 메시지)
    private fun showError(errorType: com.choo.moviefinder.core.util.ErrorType) {
        binding.progressBar.isVisible = false
        binding.contentLayout.isVisible = false
        binding.errorView.layoutError.isVisible = true

        binding.errorView.tvErrorMessage.text =
            ErrorMessageProvider.getMessage(requireContext(), errorType)
        binding.errorView.btnRetry.isVisible = false
    }

    // 바인딩 null 처리로 메모리 누수 방지
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
