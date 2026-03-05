package com.choo.moviefinder.presentation.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.ChangeImageTransform
import android.transition.ChangeTransform
import android.transition.TransitionSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.choo.moviefinder.R
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.core.util.ImageUrlProvider
import com.choo.moviefinder.databinding.FragmentDetailBinding
import com.choo.moviefinder.domain.model.MovieDetail
import com.choo.moviefinder.presentation.adapter.CastAdapter
import com.choo.moviefinder.presentation.adapter.HorizontalMovieAdapter
import com.choo.moviefinder.presentation.adapter.ReviewAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Suppress("TooManyFunctions")
@AndroidEntryPoint
class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailViewModel by viewModels()

    private val args: DetailFragmentArgs by navArgs()

    private lateinit var castAdapter: CastAdapter
    private lateinit var similarMovieAdapter: HorizontalMovieAdapter
    private lateinit var reviewAdapter: ReviewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedTransition = TransitionSet().apply {
            addTransition(ChangeBounds())
            addTransition(ChangeTransform())
            addTransition(ChangeImageTransform())
            duration = 300
        }
        sharedElementEnterTransition = sharedTransition
        sharedElementReturnTransition = sharedTransition
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Shared Element Transition: 포스터 → 배경 이미지 전환
        ViewCompat.setTransitionName(binding.ivBackdrop, "poster_${args.movieId}")
        postponeEnterTransition(500, TimeUnit.MILLISECONDS)

        setupToolbar()
        setupRecyclerViews()
        setupFab()
        observeUiState()
        observeFavorite()
        observeWatchlist()
        observeSnackbar()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.toolbar.inflateMenu(R.menu.menu_detail)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_share -> {
                    shareMovie()
                    true
                }
                else -> false
            }
        }
    }

    private fun shareMovie() {
        val state = viewModel.uiState.value
        if (state !is DetailUiState.Success) return
        val detail = state.movieDetail
        val shareText = buildString {
            append(detail.title)
            if (detail.releaseDate.isNotBlank()) {
                append(" (${detail.releaseDate})")
            }
            append("\n")
            if (detail.overview.isNotBlank()) {
                append(detail.overview)
                append("\n\n")
            }
            append("https://www.themoviedb.org/movie/${detail.id}")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_chooser_title)))
    }

    private fun setupRecyclerViews() {
        castAdapter = CastAdapter()
        binding.rvCast.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = castAdapter
        }

        // 비슷한 영화 클릭 시 같은 DetailFragment를 새 인스턴스로 재생성 (self-navigation)
        similarMovieAdapter = HorizontalMovieAdapter { movieId ->
            if (findNavController().currentDestination?.id == R.id.detailFragment) {
                findNavController().navigate(
                    R.id.action_detailFragment_self,
                    DetailFragmentArgs(movieId).toBundle()
                )
            }
        }
        binding.rvSimilar.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = similarMovieAdapter
        }

        reviewAdapter = ReviewAdapter()
        binding.rvReviews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewAdapter
        }
    }

    private fun setupFab() {
        binding.fabFavorite.setOnClickListener {
            animateFabBounce(binding.fabFavorite)
            viewModel.toggleFavorite()
        }
        binding.fabWatchlist.setOnClickListener {
            animateFabBounce(binding.fabWatchlist)
            viewModel.toggleWatchlist()
        }
    }

    private fun animateFabBounce(fab: View) {
        fab.animate().cancel()
        fab.scaleX = 1f
        fab.scaleY = 1f
        fab.animate()
            .scaleX(1.2f).scaleY(1.2f)
            .setDuration(150)
            .withEndAction {
                fab.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is DetailUiState.Loading -> showLoading()
                        is DetailUiState.Success -> showContent(state)
                        is DetailUiState.Error -> showError(state.errorType)
                    }
                }
            }
        }
    }

    private fun observeFavorite() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFavorite.collect { isFavorite ->
                    val icon = if (isFavorite) {
                        R.drawable.ic_favorite
                    } else {
                        R.drawable.ic_favorite_border
                    }
                    binding.fabFavorite.setImageResource(icon)
                }
            }
        }
    }

    private fun observeWatchlist() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isInWatchlist.collect { inWatchlist ->
                    val icon = if (inWatchlist) {
                        R.drawable.ic_watchlist
                    } else {
                        R.drawable.ic_watchlist_border
                    }
                    binding.fabWatchlist.setImageResource(icon)
                }
            }
        }
    }

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

    private fun showLoading() {
        binding.progressBar.isVisible = true
        binding.contentLayout.isVisible = false
        binding.errorView.layoutError.isVisible = false
        binding.fabFavorite.isVisible = false
        binding.fabWatchlist.isVisible = false
    }

    private fun showContent(state: DetailUiState.Success) {
        binding.progressBar.isVisible = false
        binding.contentLayout.isVisible = true
        binding.errorView.layoutError.isVisible = false
        binding.fabFavorite.isVisible = true
        binding.fabWatchlist.isVisible = true

        val detail = state.movieDetail
        bindMovieDetail(detail)
        bindTrailer(state.trailerKey)

        // Cast (self-navigation 시 이전 영화 데이터가 남지 않도록 항상 리스트를 갱신)
        castAdapter.submitList(state.credits)
        binding.tvCastLabel.isVisible = state.credits.isNotEmpty()
        binding.rvCast.isVisible = state.credits.isNotEmpty()

        // 비슷한 영화
        similarMovieAdapter.submitList(state.similarMovies)
        binding.tvSimilarLabel.isVisible = state.similarMovies.isNotEmpty()
        binding.rvSimilar.isVisible = state.similarMovies.isNotEmpty()

        // 리뷰
        reviewAdapter.submitList(state.reviews)
        binding.tvReviewsLabel.isVisible = state.reviews.isNotEmpty()
        binding.rvReviews.isVisible = state.reviews.isNotEmpty()

        // 등급 배지
        bindCertification(state.certification)
    }

    private fun bindMovieDetail(detail: MovieDetail) {
        binding.collapsingToolbar.title = detail.title

        binding.ivBackdrop.load(ImageUrlProvider.backdropUrl(detail.backdropPath)) {
            crossfade(true)
            placeholder(R.drawable.bg_poster_placeholder)
            error(R.drawable.bg_poster_placeholder)
            listener(
                onSuccess = { _, _ -> startPostponedEnterTransition() },
                onError = { _, _ -> startPostponedEnterTransition() }
            )
        }

        binding.tvTitle.text = detail.title
        binding.ratingView.setRating(detail.voteAverage)
        binding.tvReleaseDate.text = getString(R.string.release_date_format, detail.releaseDate)

        if (detail.runtime != null && detail.runtime > 0) {
            binding.tvRuntime.text = getString(R.string.runtime_format, detail.runtime)
            binding.tvRuntime.isVisible = true
        } else {
            binding.tvRuntime.isVisible = false
        }

        if (!detail.tagline.isNullOrBlank()) {
            binding.tvTagline.text = detail.tagline
            binding.tvTagline.isVisible = true
        } else {
            binding.tvTagline.isVisible = false
        }

        binding.tvOverview.text = detail.overview

        // 장르 칩 동적 추가
        binding.chipGroupGenres.removeAllViews()
        detail.genres.forEach { genre ->
            val chip = Chip(requireContext()).apply {
                text = genre.name
                isClickable = false
                isCheckable = false
            }
            binding.chipGroupGenres.addView(chip)
        }

        // 확장 정보
        bindExtendedInfo(detail)
    }

    private fun bindExtendedInfo(detail: MovieDetail) {
        val hasInfo = detail.status.isNotBlank() || detail.originalLanguage.isNotBlank() ||
            detail.budget > 0 || detail.revenue > 0 || !detail.imdbId.isNullOrBlank()

        binding.tvMovieInfoLabel.isVisible = hasInfo
        binding.movieInfoSection.isVisible = hasInfo

        if (!hasInfo) return

        bindOptionalField(
            binding.tvStatus,
            detail.status.isNotBlank(),
            getString(R.string.status_format, detail.status)
        )
        bindOptionalField(
            binding.tvOriginalLanguage,
            detail.originalLanguage.isNotBlank(),
            getString(R.string.original_language_format, detail.originalLanguage.uppercase())
        )
        bindOptionalField(binding.tvBudget, detail.budget > 0, getString(R.string.budget_format, detail.budget))
        bindOptionalField(binding.tvRevenue, detail.revenue > 0, getString(R.string.revenue_format, detail.revenue))

        if (!detail.imdbId.isNullOrBlank()) {
            binding.btnImdb.isVisible = true
            binding.btnImdb.setOnClickListener {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://www.imdb.com/title/${detail.imdbId}".toUri()
                )
                try {
                    startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    Snackbar.make(
                        binding.root,
                        R.string.error_no_browser,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            binding.btnImdb.isVisible = false
        }
    }

    private fun bindOptionalField(textView: TextView, show: Boolean, text: String) {
        textView.isVisible = show
        if (show) textView.text = text
    }

    private fun bindCertification(certification: String?) {
        if (!certification.isNullOrBlank()) {
            binding.chipCertification.text = certification
            binding.chipCertification.isVisible = true
        } else {
            binding.chipCertification.isVisible = false
        }
    }

    private fun bindTrailer(trailerKey: String?) {
        if (trailerKey != null) {
            binding.btnTrailer.isVisible = true
            binding.btnTrailer.setOnClickListener {
                // YouTube 앱 또는 웹으로 연결
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://www.youtube.com/watch?v=$trailerKey".toUri()
                )
                try {
                    startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    Snackbar.make(
                        binding.root,
                        R.string.error_no_browser,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            binding.btnTrailer.isVisible = false
        }
    }

    private fun showError(errorType: ErrorType) {
        startPostponedEnterTransition()
        binding.progressBar.isVisible = false
        binding.contentLayout.isVisible = false
        binding.errorView.layoutError.isVisible = true
        binding.fabFavorite.isVisible = false
        binding.fabWatchlist.isVisible = false
        binding.errorView.btnRetry.isEnabled = true

        binding.errorView.tvErrorMessage.text =
            ErrorMessageProvider.getMessage(requireContext(), errorType)
        binding.errorView.btnRetry.setOnClickListener {
            binding.errorView.btnRetry.isEnabled = false
            viewModel.loadMovieDetail()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvCast.adapter = null
        binding.rvSimilar.adapter = null
        binding.rvReviews.adapter = null
        _binding = null
    }
}
