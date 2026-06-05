package com.choo.moviefinder.presentation.adapter

import android.view.View
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import coil3.dispose
import com.choo.moviefinder.R
import com.choo.moviefinder.databinding.ItemMovieListBinding
import com.choo.moviefinder.domain.model.Movie
import java.util.Locale

class MovieListViewHolder(
    private val binding: ItemMovieListBinding,
    private val onMovieClick: (Int, View) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    // 영화 제목, 개봉일, 줄거리, 평점, 포스터 이미지 및 클릭 리스너 바인딩
    fun bind(movie: Movie) {
        binding.tvTitle.text = movie.title
        binding.tvReleaseDate.text = movie.releaseDate
        binding.tvOverview.text = movie.overview
        binding.ratingView.setRating(movie.voteAverage)
        binding.tvVoteCount.text = String.format(Locale.US, "(%,d)", movie.voteCount)

        binding.cardMovie.contentDescription = binding.root.context.getString(
            R.string.cd_movie_item, movie.title, movie.voteAverage, movie.releaseDate
        )

        ViewCompat.setTransitionName(binding.ivPoster, "poster_${movie.id}")

        binding.ivPoster.loadPoster(movie.posterPath)

        binding.cardMovie.setOnClickListener {
            onMovieClick(movie.id, binding.ivPoster)
        }
    }

    // Coil 이미지 로드 취소
    fun recycle() {
        binding.ivPoster.dispose()
    }
}
