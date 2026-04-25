package com.choo.moviefinder.presentation.collection

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.choo.moviefinder.R
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.core.util.ImageUrlProvider
import com.choo.moviefinder.core.util.RateLimiter
import com.choo.moviefinder.databinding.FragmentCollectionBinding
import com.choo.moviefinder.domain.model.CollectionDetail
import com.choo.moviefinder.presentation.adapter.HorizontalMovieAdapter
import com.choo.moviefinder.presentation.detail.DetailFragmentArgs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CollectionFragment : Fragment() {

    private var _binding: FragmentCollectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CollectionViewModel by viewModels()

    private lateinit var moviesAdapter: HorizontalMovieAdapter
    private val retryRateLimiter = RateLimiter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        observeUiState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        moviesAdapter = HorizontalMovieAdapter(onMovieClick = { movieId ->
            if (findNavController().currentDestination?.id == R.id.collectionFragment) {
                findNavController().navigate(
                    R.id.action_collectionFragment_to_detailFragment,
                    DetailFragmentArgs(movieId).toBundle()
                )
            }
        })
        binding.rvCollectionMovies.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            setHasFixedSize(true)
            itemAnimator = null
            adapter = moviesAdapter
        }
    }

    private fun observeUiState() {
        binding.shimmerView.shimmerLayout.startShimmer()
        binding.shimmerView.shimmerLayout.isVisible = true
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val shimmer = binding.shimmerView.shimmerLayout
                    val isLoading = state is CollectionUiState.Loading
                    shimmer.isVisible = isLoading
                    if (isLoading) shimmer.startShimmer() else shimmer.stopShimmer()

                    binding.errorView.root.isVisible = state is CollectionUiState.Error
                    binding.tvEmpty.isVisible = false

                    val isSuccess = state is CollectionUiState.Success
                    binding.tvCollectionName.isVisible = isSuccess
                    binding.tvOverview.isVisible = isSuccess
                    binding.tvMoviesHeader.isVisible = isSuccess
                    binding.rvCollectionMovies.isVisible = isSuccess

                    when (state) {
                        is CollectionUiState.Success -> {
                            bindCollection(state.collection)
                            binding.tvEmpty.isVisible = state.collection.movies.isEmpty()
                            binding.tvMoviesHeader.isVisible = state.collection.movies.isNotEmpty()
                        }
                        is CollectionUiState.Error -> {
                            binding.errorView.tvErrorMessage.text =
                                ErrorMessageProvider.getMessage(requireContext(), state.errorType)
                            binding.errorView.btnRetry.setOnClickListener {
                                if (retryRateLimiter.tryAcquire()) viewModel.loadCollection()
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun bindCollection(collection: CollectionDetail) {
        binding.toolbar.title = collection.name
        binding.tvCollectionName.text = collection.name
        binding.tvOverview.isVisible = collection.overview.isNotBlank()
        binding.tvOverview.text = collection.overview

        collection.backdropPath?.let { path ->
            binding.ivBackdrop.load(ImageUrlProvider.backdropUrl(path)) {
                crossfade(true)
                placeholder(R.drawable.bg_poster_placeholder)
                error(R.drawable.bg_poster_placeholder)
                size(coil3.size.ViewSizeResolver(binding.ivBackdrop))
            }
        }

        moviesAdapter.submitList(collection.movies)
    }

    override fun onDestroyView() {
        binding.rvCollectionMovies.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
