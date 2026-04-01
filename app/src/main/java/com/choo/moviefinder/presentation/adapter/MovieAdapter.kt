package com.choo.moviefinder.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.choo.moviefinder.databinding.ItemMovieGridBinding
import com.choo.moviefinder.domain.model.Movie

class MovieAdapter(
    private val onMovieClick: (Int, View) -> Unit,
    private val onMovieLongClick: ((Movie) -> Unit)? = null
) : ListAdapter<Movie, MovieGridViewHolder>(MovieDiffCallback) {

    // 그리드 뷰 ViewHolder 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieGridViewHolder {
        val binding = ItemMovieGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MovieGridViewHolder(binding, onMovieClick, onMovieLongClick)
    }

    // 영화 데이터를 ViewHolder에 바인딩
    override fun onBindViewHolder(holder: MovieGridViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    // 재활용 시 Coil 이미지 로드 취소
    override fun onViewRecycled(holder: MovieGridViewHolder) {
        super.onViewRecycled(holder)
        holder.recycle()
    }
}
