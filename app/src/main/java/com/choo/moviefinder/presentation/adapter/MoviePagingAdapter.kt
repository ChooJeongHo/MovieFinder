package com.choo.moviefinder.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.choo.moviefinder.core.util.ImageUrlProvider
import com.choo.moviefinder.databinding.ItemMovieGridBinding
import com.choo.moviefinder.domain.model.Movie

class MoviePagingAdapter(
    private val onMovieClick: (Int, View) -> Unit
) : PagingDataAdapter<Movie, MoviePagingAdapter.MovieViewHolder>(MovieDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = getItem(position) ?: return
        holder.bind(movie)
    }

    inner class MovieViewHolder(
        private val binding: ItemMovieGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            binding.tvTitle.text = movie.title
            binding.tvReleaseDate.text = movie.releaseDate
            binding.ratingView.setRating(movie.voteAverage)

            ViewCompat.setTransitionName(binding.ivPoster, "poster_${movie.id}")

            binding.ivPoster.load(ImageUrlProvider.posterUrl(movie.posterPath)) {
                crossfade(true)
                placeholder(com.choo.moviefinder.R.drawable.bg_poster_placeholder)
                error(com.choo.moviefinder.R.drawable.bg_poster_placeholder)
            }

            binding.cardMovie.setOnClickListener {
                onMovieClick(movie.id, binding.ivPoster)
            }
        }
    }

}
