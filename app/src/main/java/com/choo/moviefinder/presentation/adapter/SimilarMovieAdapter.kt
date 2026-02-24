package com.choo.moviefinder.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.dispose
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.choo.moviefinder.core.util.ImageUrlProvider
import com.choo.moviefinder.databinding.ItemMovieHorizontalBinding
import com.choo.moviefinder.domain.model.Movie

class SimilarMovieAdapter(
    private val onMovieClick: (Int) -> Unit
) : ListAdapter<Movie, SimilarMovieAdapter.SimilarMovieViewHolder>(MovieDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimilarMovieViewHolder {
        val binding = ItemMovieHorizontalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SimilarMovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SimilarMovieViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: SimilarMovieViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.ivPoster.dispose()
    }

    inner class SimilarMovieViewHolder(
        val binding: ItemMovieHorizontalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            ViewCompat.setTransitionName(binding.ivPoster, "poster_${movie.id}")
            binding.tvTitle.text = movie.title

            binding.ivPoster.load(ImageUrlProvider.posterUrl(movie.posterPath)) {
                crossfade(true)
                placeholder(com.choo.moviefinder.R.drawable.bg_poster_placeholder)
                error(com.choo.moviefinder.R.drawable.bg_poster_placeholder)
            }

            binding.cardMovie.setOnClickListener {
                onMovieClick(movie.id)
            }
        }
    }

}
