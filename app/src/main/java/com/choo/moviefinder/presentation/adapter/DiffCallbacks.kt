package com.choo.moviefinder.presentation.adapter

import androidx.recyclerview.widget.DiffUtil
import com.choo.moviefinder.domain.model.Movie

object MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
    override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean =
        oldItem == newItem
}
