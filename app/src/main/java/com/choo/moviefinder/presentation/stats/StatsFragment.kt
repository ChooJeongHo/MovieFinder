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

        bindGoalProgress(stats)

        if (stats.averageRating != null) {
            binding.tvAverageRating.text = getString(R.string.stats_rating_format, stats.averageRating)
        } else {
            binding.tvAverageRating.text = getString(R.string.stats_no_rating)
        }

        bindTopGenres(stats)
        bindGenrePieChart(stats)
        bindMonthlyBarChart(stats)
        bindRatingHistogram(stats)
        bindCalendarHeatmap(stats)
    }

    // 장르 Top 3 텍스트 바인딩
    private fun bindTopGenres(stats: WatchStats) {
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

    // 장르별 시청 비율 파이차트 바인딩
    private fun bindGenrePieChart(stats: WatchStats) {
        if (stats.allGenreCounts.isEmpty()) {
            binding.pieChartView.isVisible = false
            binding.tvPieChartEmpty.isVisible = true
        } else {
            binding.pieChartView.isVisible = true
            binding.tvPieChartEmpty.isVisible = false
            binding.pieChartView.setData(
                stats.allGenreCounts.map { it.name to it.count }
            )
        }
    }

    // 시청 목표 달성률 바인딩
    private fun bindGoalProgress(stats: WatchStats) {
        if (stats.monthlyWatchGoal <= 0) {
            binding.cardGoalProgress.isVisible = false
            return
        }
        binding.cardGoalProgress.isVisible = true

        val progress = (stats.monthlyWatched.toFloat() / stats.monthlyWatchGoal * 100).toInt().coerceAtMost(100)
        binding.tvGoalProgress.text = getString(
            R.string.stats_goal_progress_format,
            stats.monthlyWatched,
            stats.monthlyWatchGoal
        )
        binding.progressGoal.progress = progress

        if (stats.monthlyWatched >= stats.monthlyWatchGoal) {
            binding.tvGoalStatus.text = getString(R.string.stats_goal_achieved)
            binding.tvGoalStatus.setTextColor(requireContext().getColor(R.color.colorPrimary))
        } else {
            val remaining = stats.monthlyWatchGoal - stats.monthlyWatched
            binding.tvGoalStatus.text = getString(R.string.stats_goal_remaining, remaining)
            val typedValue = android.util.TypedValue()
            requireContext().theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
            binding.tvGoalStatus.setTextColor(
                requireContext().getColor(typedValue.resourceId)
            )
        }
    }

    // 월별 시청 편수 바차트 바인딩
    private fun bindMonthlyBarChart(stats: WatchStats) {
        if (stats.monthlyWatchCounts.isEmpty()) {
            binding.barChartView.isVisible = false
            binding.tvBarChartEmpty.isVisible = true
        } else {
            binding.barChartView.isVisible = true
            binding.tvBarChartEmpty.isVisible = false
            binding.barChartView.setData(
                stats.monthlyWatchCounts.map { it.yearMonth to it.count }
            )
        }
    }

    // 평점 분포 히스토그램 바인딩
    private fun bindRatingHistogram(stats: WatchStats) {
        if (stats.ratingDistribution.isEmpty()) {
            binding.histogramView.isVisible = false
            binding.tvHistogramEmpty.isVisible = true
        } else {
            binding.histogramView.isVisible = true
            binding.tvHistogramEmpty.isVisible = false
            binding.histogramView.setData(stats.ratingDistribution)
        }
    }

    // 시청 캘린더 히트맵 바인딩
    private fun bindCalendarHeatmap(stats: WatchStats) {
        if (stats.dailyWatchCounts.isEmpty()) {
            binding.calendarHeatmapView.isVisible = false
            binding.tvCalendarEmpty.isVisible = true
        } else {
            binding.calendarHeatmapView.isVisible = true
            binding.tvCalendarEmpty.isVisible = false
            binding.calendarHeatmapView.setData(stats.dailyWatchCounts)
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
