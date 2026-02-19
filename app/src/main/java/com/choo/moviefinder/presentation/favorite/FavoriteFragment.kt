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
import com.choo.moviefinder.presentation.adapter.MovieAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoriteFragment : Fragment() {

    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoriteViewModel by viewModels()

    private lateinit var movieAdapter: MovieAdapter

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
        setupEmptyState()
        setupRecyclerView()
        setupSwipeToDelete()
        observeFavorites()
    }

    private fun setupEmptyState() {
        binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_favorite_border)
        binding.emptyView.tvEmptyTitle.text = getString(R.string.favorite_empty_title)
        binding.emptyView.tvEmptyMessage.text = getString(R.string.favorite_empty_message)
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
                val movie = movieAdapter.currentList[position]
                viewModel.toggleFavorite(movie)
                Snackbar.make(binding.root, getString(R.string.favorite_removed), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.undo)) {
                        viewModel.toggleFavorite(movie)
                    }
                    .show()
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.rvFavorites)
    }

    private fun observeFavorites() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favoriteMovies.collect { movies ->
                    movieAdapter.submitList(movies)
                    binding.rvFavorites.isVisible = movies.isNotEmpty()
                    binding.emptyView.layoutEmpty.isVisible = movies.isEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvFavorites.adapter = null
        _binding = null
    }
}
