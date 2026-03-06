package com.choo.moviefinder.presentation.adapter

import androidx.recyclerview.widget.DiffUtil
import com.choo.moviefinder.domain.model.Movie

object MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
    // 동일 영화인지 ID로 비교
    override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean =
        oldItem.id == newItem.id

    // 영화 데이터 내용이 동일한지 비교
    override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean =
        oldItem == newItem
}
