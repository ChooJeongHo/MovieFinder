package com.choo.moviefinder.presentation.detail

import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.ChangeImageTransform
import android.transition.ChangeTransform
import android.transition.TransitionSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
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
import com.choo.moviefinder.core.util.ImageUrlProvider
import com.choo.moviefinder.databinding.FragmentDetailBinding
import com.choo.moviefinder.domain.model.MovieDetail
import com.choo.moviefinder.presentation.adapter.CastAdapter
import com.choo.moviefinder.presentation.adapter.SimilarMovieAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailViewModel by viewModels()

    private val args: DetailFragmentArgs by navArgs()

    private lateinit var castAdapter: CastAdapter
    private lateinit var similarMovieAdapter: SimilarMovieAdapter

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
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        setupToolbar()
        setupRecyclerViews()
        setupFab()
        observeUiState()
        observeFavorite()
        observeSnackbar()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerViews() {
        castAdapter = CastAdapter()
        binding.rvCast.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = castAdapter
        }

        // 비슷한 영화 클릭 시 같은 DetailFragment를 새 인스턴스로 재생성 (self-navigation)
        similarMovieAdapter = SimilarMovieAdapter { movieId ->
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
    }

    private fun setupFab() {
        binding.fabFavorite.setOnClickListener {
            viewModel.toggleFavorite()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is DetailUiState.Loading -> showLoading()
                        is DetailUiState.Success -> showContent(state)
                        is DetailUiState.Error -> showError(state.message)
                    }
                }
            }
        }
    }

    private fun observeFavorite() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFavorite.collect { isFavorite ->
                    binding.fabFavorite.setImageResource(
                        if (isFavorite) R.drawable.ic_favorite
                        else R.drawable.ic_favorite_border
                    )
                }
            }
        }
    }

    private fun observeSnackbar() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.snackbarMessage.collect { message ->
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
    }

    private fun showContent(state: DetailUiState.Success) {
        binding.progressBar.isVisible = false
        binding.contentLayout.isVisible = true
        binding.errorView.layoutError.isVisible = false
        binding.fabFavorite.isVisible = true

        val detail = state.movieDetail
        bindMovieDetail(detail)
        bindTrailer(state.trailerKey)

        // Cast (self-navigation 시 이전 영화 데이터가 남지 않도록 항상 visibility를 재설정)
        if (state.credits.isNotEmpty()) {
            binding.tvCastLabel.isVisible = true
            binding.rvCast.isVisible = true
            castAdapter.submitList(state.credits)
        } else {
            binding.tvCastLabel.isVisible = false
            binding.rvCast.isVisible = false
        }

        // Similar movies
        if (state.similarMovies.isNotEmpty()) {
            binding.tvSimilarLabel.isVisible = true
            binding.rvSimilar.isVisible = true
            similarMovieAdapter.submitList(state.similarMovies)
        } else {
            binding.tvSimilarLabel.isVisible = false
            binding.rvSimilar.isVisible = false
        }
    }

    private fun bindMovieDetail(detail: MovieDetail) {
        binding.collapsingToolbar.title = detail.title

        binding.ivBackdrop.load(ImageUrlProvider.backdropUrl(detail.backdropPath)) {
            crossfade(true)
            placeholder(R.drawable.bg_poster_placeholder)
            error(R.drawable.bg_poster_placeholder)
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

        // Genres
        binding.chipGroupGenres.removeAllViews()
        detail.genres.forEach { genre ->
            val chip = Chip(requireContext()).apply {
                text = genre.name
                isClickable = false
                isCheckable = false
            }
            binding.chipGroupGenres.addView(chip)
        }
    }

    private fun bindTrailer(trailerKey: String?) {
        if (trailerKey != null) {
            binding.btnTrailer.isVisible = true
            binding.btnTrailer.setOnClickListener {
                TrailerDialogFragment.newInstance(trailerKey)
                    .show(childFragmentManager, "trailer")
            }
        } else {
            binding.btnTrailer.isVisible = false
        }
    }

    private fun showError(message: String) {
        binding.progressBar.isVisible = false
        binding.contentLayout.isVisible = false
        binding.errorView.layoutError.isVisible = true
        binding.fabFavorite.isVisible = false

        binding.errorView.tvErrorMessage.text = message
        binding.errorView.btnRetry.setOnClickListener {
            viewModel.loadMovieDetail()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvCast.adapter = null
        binding.rvSimilar.adapter = null
        _binding = null
    }
}
