package com.choo.moviefinder.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.choo.moviefinder.databinding.ItemMovieGridBinding
import com.choo.moviefinder.domain.model.Movie

class MoviePagingAdapter(
    private val onMovieClick: (Int, View) -> Unit
) : PagingDataAdapter<Movie, MovieGridViewHolder>(MovieDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieGridViewHolder {
        val binding = ItemMovieGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MovieGridViewHolder(binding, onMovieClick)
    }

    override fun onBindViewHolder(holder: MovieGridViewHolder, position: Int) {
        val movie = getItem(position) ?: return
        holder.bind(movie)
    }

    override fun onViewRecycled(holder: MovieGridViewHolder) {
        super.onViewRecycled(holder)
        holder.recycle()
    }
}