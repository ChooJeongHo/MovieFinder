package com.choo.moviefinder.presentation.favorite

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
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.choo.moviefinder.R
import com.choo.moviefinder.databinding.FragmentFavoriteBinding
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.presentation.adapter.MovieAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoriteFragment : Fragment() {

    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoriteViewModel by viewModels()

    private lateinit var movieAdapter: MovieAdapter
    private var currentTab = TAB_FAVORITES
    private var collectJob: Job? = null
    private var swipeHelper: ItemTouchHelper? = null

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
        savedInstanceState?.let { currentTab = it.getInt(KEY_CURRENT_TAB, TAB_FAVORITES) }
        setupRecyclerView()
        setupSwipeToDelete()
        setupTabs()
        collectCurrentTab()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_TAB, currentTab)
    }

    private fun setupRecyclerView() {
        movieAdapter = MovieAdapter { movieId, posterView ->
            if (findNavController().currentDestination?.id == R.id.favoriteFragment) {
                val action = FavoriteFragmentDirections.actionFavoriteToDetail(movieId)
                val extras = FragmentNavigatorExtras(posterView to "poster_$movieId")
                findNavController().navigate(action, extras)
            }
        }

        binding.rvFavorites.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = movieAdapter
        }
    }

    private fun setupSwipeToDelete() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

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
                .setAction(getString(R.string.undo)) {
                    viewModel.toggleFavorite(movie)
                }
                .show()
        } else {
            viewModel.toggleWatchlist(movie)
            Snackbar.make(binding.root, getString(R.string.watchlist_removed), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.undo)) {
                    viewModel.toggleWatchlist(movie)
                }
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
                collectCurrentTab()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun collectCurrentTab() {
        collectJob?.cancel()
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
