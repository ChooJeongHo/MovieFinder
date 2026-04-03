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
import coil3.size.ViewSizeResolver
import com.choo.moviefinder.R
import com.choo.moviefinder.core.util.ImageUrlProvider
import com.choo.moviefinder.databinding.ItemMovieHorizontalBinding
import com.choo.moviefinder.domain.model.Movie

class HorizontalMovieAdapter(
    private val transitionPrefix: String = "poster",
    private val onMovieClick: (Int) -> Unit
) : ListAdapter<Movie, HorizontalMovieAdapter.HorizontalMovieViewHolder>(MovieDiffCallback) {

    // 가로 스크롤 영화 카드 ViewHolder 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorizontalMovieViewHolder {
        val binding = ItemMovieHorizontalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HorizontalMovieViewHolder(binding)
    }

    // 영화 데이터를 ViewHolder에 바인딩
    override fun onBindViewHolder(holder: HorizontalMovieViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    // 재활용 시 Coil 포스터 이미지 로드 취소
    override fun onViewRecycled(holder: HorizontalMovieViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.ivPoster.dispose()
    }

    inner class HorizontalMovieViewHolder(
        val binding: ItemMovieHorizontalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // 영화 제목, 포스터, 클릭 리스너 및 Shared Element 전환 이름 바인딩
        fun bind(movie: Movie) {
            ViewCompat.setTransitionName(
                binding.ivPoster,
                "${transitionPrefix}_${movie.id}"
            )
            binding.tvTitle.text = movie.title
            binding.cardMovie.contentDescription = movie.title

            binding.ivPoster.load(ImageUrlProvider.posterUrl(movie.posterPath)) {
                crossfade(true)
                placeholder(R.drawable.bg_poster_placeholder)
                error(R.drawable.bg_poster_placeholder)
                size(ViewSizeResolver(binding.ivPoster))
            }

            binding.cardMovie.setOnClickListener {
                onMovieClick(movie.id)
            }
        }
    }
}
