package com.choo.moviefinder.presentation.stats

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.choo.moviefinder.R
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.core.util.RateLimiter
import com.choo.moviefinder.databinding.FragmentStatsBinding
import com.choo.moviefinder.domain.model.WatchStats
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Suppress("TooManyFunctions")
@AndroidEntryPoint
class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatsViewModel by viewModels()
    private val rateLimiter = RateLimiter()

    private var currentStats: WatchStats? = null

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

    // 툴바 뒤로가기 버튼 및 메뉴 설정
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.toolbar.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_stats, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_share_stats -> {
                        currentStats?.let { shareStatsImage(it) }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
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
        binding.shimmerStats.shimmerLayout.isVisible = true
        binding.shimmerStats.shimmerLayout.startShimmer()
        binding.contentLayout.isVisible = false
        binding.errorView.layoutError.isVisible = false
    }

    // 성공 상태 표시 (통계 데이터 바인딩)
    private fun showContent(stats: WatchStats) {
        currentStats = stats
        binding.shimmerStats.shimmerLayout.stopShimmer()
        binding.shimmerStats.shimmerLayout.isVisible = false
        binding.contentLayout.isVisible = true
        binding.errorView.layoutError.isVisible = false

        binding.tvTotalWatched.text = getString(R.string.stats_count_format, stats.totalWatched)
        binding.tvMonthlyWatched.text = getString(R.string.stats_count_format, stats.monthlyWatched)

        bindGoalProgress(stats)

        binding.tvAverageRating.text = stats.averageRating
            ?.let { getString(R.string.stats_rating_format, it) }
            ?: getString(R.string.stats_no_rating)

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
            val typedValue = TypedValue()
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
        binding.shimmerStats.shimmerLayout.stopShimmer()
        binding.shimmerStats.shimmerLayout.isVisible = false
        binding.contentLayout.isVisible = false
        binding.errorView.layoutError.isVisible = true

        binding.errorView.tvErrorMessage.text =
            ErrorMessageProvider.getMessage(requireContext(), errorType)
        binding.errorView.btnRetry.isVisible = true
        binding.errorView.btnRetry.setOnClickListener {
            if (!rateLimiter.tryAcquire()) return@setOnClickListener
            viewModel.retry()
        }
    }

    // 테마 색상을 메인 스레드에서 미리 해석한 데이터 클래스
    private data class StatsCardColors(
        val primaryColor: Int,
        val surfaceColor: Int,
        val textPrimaryColor: Int,
        val textSecondaryColor: Int
    )

    // 메인 스레드에서 테마 색상을 해석하여 반환
    private fun resolveStatsCardColors(): StatsCardColors {
        val typedValue = TypedValue()
        val theme = requireContext().theme
        val primaryColor = requireContext().getColor(R.color.colorPrimary)
        theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true)
        val surfaceColor = if (typedValue.resourceId != 0) {
            requireContext().getColor(typedValue.resourceId)
        } else {
            typedValue.data
        }
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        val textPrimaryColor = if (typedValue.resourceId != 0) {
            requireContext().getColor(typedValue.resourceId)
        } else {
            typedValue.data
        }
        theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
        val textSecondaryColor = if (typedValue.resourceId != 0) {
            requireContext().getColor(typedValue.resourceId)
        } else {
            typedValue.data
        }
        return StatsCardColors(primaryColor, surfaceColor, textPrimaryColor, textSecondaryColor)
    }

    // 시청 통계 공유 이미지를 생성하고 Intent.ACTION_SEND로 공유 (백그라운드에서 비트맵 생성)
    private fun shareStatsImage(stats: WatchStats) {
        // requireContext()를 suspend 이전에 캡처하여 Fragment detach 후 접근 방지
        val ctx = requireContext()
        // 테마 색상과 문자열은 메인 스레드에서 미리 해석
        val colors = resolveStatsCardColors()
        val cardTitle = getString(R.string.stats_share_card_title)
        val totalText = getString(R.string.stats_share_total, stats.totalWatched)
        val monthlyText = getString(R.string.stats_share_monthly, stats.monthlyWatched)
        val ratingText = stats.averageRating?.let { getString(R.string.stats_share_rating, it) }
        val genreText = if (stats.topGenres.isNotEmpty()) {
            val names = stats.topGenres.take(3).joinToString(", ") { it.name }
            getString(R.string.stats_share_genres, names)
        } else {
            null
        }
        val goalText = if (stats.monthlyWatchGoal > 0) {
            getString(R.string.stats_goal_progress_format, stats.monthlyWatched, stats.monthlyWatchGoal)
        } else {
            null
        }
        val shareTitle = getString(R.string.stats_share_title)
        val packageName = ctx.packageName
        val cacheDir = ctx.cacheDir

        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                createStatsCardBitmap(colors, cardTitle, totalText, monthlyText, ratingText, genreText, goalText)
            }
            try {
                val shareFile = withContext(Dispatchers.IO) {
                    val shareDir = File(cacheDir, "share_images")
                    shareDir.mkdirs()
                    val file = File(shareDir, "stats_card.png")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    file
                }
                val uri = FileProvider.getUriForFile(ctx, "$packageName.fileprovider", shareFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                if (!isAdded) return@launch
                startActivity(Intent.createChooser(intent, shareTitle))
            } finally {
                bitmap.recycle()
            }
        }
    }

    // Canvas 기반 통계 카드 Bitmap 생성 (백그라운드 스레드에서 호출 가능)
    @Suppress("LongMethod", "LongParameterList")
    private fun createStatsCardBitmap(
        colors: StatsCardColors,
        cardTitle: String,
        totalText: String,
        monthlyText: String,
        ratingText: String?,
        genreText: String?,
        goalText: String?
    ): Bitmap {
        val cardWidth = 800
        val padding = 48
        val lineHeight = 52

        // 카드에 표시할 줄 수 계산 (제목 + 총 시청 + 이번 달 + 평점(옵션) + 장르(옵션) + 목표(옵션))
        var lineCount = 3 // 제목 + 총시청 + 이번달
        if (ratingText != null) lineCount++
        if (genreText != null) lineCount++
        if (goalText != null) lineCount++

        val cardHeight = padding * 2 + lineHeight * lineCount + 24

        val bitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 배경 (둥근 모서리 카드)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.surfaceColor }
        val cornerRadius = 24f
        canvas.drawRoundRect(
            RectF(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat()),
            cornerRadius, cornerRadius, bgPaint
        )

        // 상단 강조 바
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.primaryColor }
        canvas.drawRoundRect(
            RectF(0f, 0f, cardWidth.toFloat(), 8f),
            cornerRadius, cornerRadius, accentPaint
        )

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.primaryColor
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.textPrimaryColor
            textSize = 28f
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.textSecondaryColor
            textSize = 24f
        }

        var y = padding.toFloat() + 36f

        // 제목
        canvas.drawText(cardTitle, padding.toFloat(), y, titlePaint)
        y += lineHeight

        // 구분선
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.textSecondaryColor
            alpha = 60
            strokeWidth = 1f
        }
        val dividerStartX = padding.toFloat()
        val dividerEndX = (cardWidth - padding).toFloat()
        canvas.drawLine(dividerStartX, y - lineHeight / 2f, dividerEndX, y - lineHeight / 2f, dividerPaint)

        // 총 시청 편수
        canvas.drawText(totalText, padding.toFloat(), y, bodyPaint)
        y += lineHeight

        // 이번 달 시청 편수
        canvas.drawText(monthlyText, padding.toFloat(), y, bodyPaint)
        y += lineHeight

        // 평균 별점
        if (ratingText != null) {
            canvas.drawText(ratingText, padding.toFloat(), y, bodyPaint)
            y += lineHeight
        }

        // 선호 장르 Top 3
        if (genreText != null) {
            canvas.drawText(genreText, padding.toFloat(), y, bodyPaint)
            y += lineHeight
        }

        // 시청 목표 달성률
        if (goalText != null) {
            canvas.drawText(goalText, padding.toFloat(), y, labelPaint)
        }

        return bitmap
    }

    // 바인딩 및 통계 참조 null 처리로 메모리 누수 방지
    override fun onDestroyView() {
        super.onDestroyView()
        currentStats = null
        _binding = null
    }
}
