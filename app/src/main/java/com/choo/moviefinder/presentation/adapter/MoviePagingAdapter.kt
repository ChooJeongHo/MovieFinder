package com.choo.moviefinder.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.choo.moviefinder.databinding.ItemMovieGridBinding
import com.choo.moviefinder.databinding.ItemMovieListBinding
import com.choo.moviefinder.domain.model.Movie

class MoviePagingAdapter(
    private val onMovieClick: (Int, View) -> Unit
) : PagingDataAdapter<Movie, RecyclerView.ViewHolder>(MovieDiffCallback) {

    var viewMode: ViewMode = ViewMode.GRID
        set(value) {
            if (field != value) {
                field = value
                @Suppress("NotifyDataSetChanged")
                notifyDataSetChanged()
            }
        }

    // 현재 보기 모드에 따른 뷰 타입 반환
    override fun getItemViewType(position: Int): Int = viewMode.ordinal

    // 뷰 타입에 따라 그리드 또는 리스트 ViewHolder 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ViewMode.LIST.ordinal) {
            val binding = ItemMovieListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            MovieListViewHolder(binding, onMovieClick)
        } else {
            val binding = ItemMovieGridBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            MovieGridViewHolder(binding, onMovieClick)
        }
    }

    // ViewHolder 타입에 따라 영화 데이터 바인딩
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val movie = getItem(position) ?: return
        when (holder) {
            is MovieGridViewHolder -> holder.bind(movie)
            is MovieListViewHolder -> holder.bind(movie)
        }
    }

    // 재활용 시 Coil 이미지 로드 취소
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        when (holder) {
            is MovieGridViewHolder -> holder.recycle()
            is MovieListViewHolder -> holder.recycle()
        }
    }

    enum class ViewMode {
        GRID,
        LIST,
    }

    companion object {
        val VIEW_TYPE_GRID = ViewMode.GRID.ordinal
        val VIEW_TYPE_LIST = ViewMode.LIST.ordinal
    }
}
