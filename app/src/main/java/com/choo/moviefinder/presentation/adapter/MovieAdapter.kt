package com.choo.moviefinder.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.choo.moviefinder.databinding.ItemMovieGridBinding
import com.choo.moviefinder.domain.model.Movie

class MovieAdapter(
    private val onMovieClick: (Int, View) -> Unit
) : ListAdapter<Movie, MovieGridViewHolder>(MovieDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieGridViewHolder {
        val binding = ItemMovieGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MovieGridViewHolder(binding, onMovieClick)
    }

    override fun onBindViewHolder(holder: MovieGridViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: MovieGridViewHolder) {
        super.onViewRecycled(holder)
        holder.recycle()
    }
}