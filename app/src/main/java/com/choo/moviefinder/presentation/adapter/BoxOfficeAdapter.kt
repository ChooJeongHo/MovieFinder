package com.choo.moviefinder.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.dispose
import com.choo.moviefinder.R
import com.choo.moviefinder.databinding.ItemBoxOfficeBinding
import com.choo.moviefinder.domain.model.BoxOfficeMovie
import java.text.NumberFormat
import java.util.Locale

class BoxOfficeAdapter(
    private val onItemClick: (BoxOfficeMovie) -> Unit
) : ListAdapter<BoxOfficeMovie, BoxOfficeAdapter.BoxOfficeViewHolder>(BoxOfficeDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxOfficeViewHolder {
        val binding = ItemBoxOfficeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BoxOfficeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BoxOfficeViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    override fun onViewRecycled(holder: BoxOfficeViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.ivPoster.dispose()
    }

    inner class BoxOfficeViewHolder(
        val binding: ItemBoxOfficeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BoxOfficeMovie) {
            val context = binding.root.context
            val boxOffice = item.boxOffice
            val movie = item.matchedMovie

            binding.tvRank.text = boxOffice.rank.toString()
            binding.tvTitle.text = boxOffice.movieName
            binding.tvAudience.text = context.getString(
                R.string.box_office_audience_format,
                NumberFormat.getNumberInstance(Locale.KOREA).format(boxOffice.audienceCount)
            )
            binding.tvRankChange.text = when {
                boxOffice.isNewEntry -> context.getString(R.string.box_office_rank_new)
                boxOffice.rankChange > 0 -> context.getString(R.string.box_office_rank_up, boxOffice.rankChange)
                boxOffice.rankChange < 0 -> context.getString(R.string.box_office_rank_down, -boxOffice.rankChange)
                else -> context.getString(R.string.box_office_rank_same)
            }

            // TMDB 매칭 성공 시에만 포스터/평점 표시, 실패 시 플레이스홀더 + 평점 배지 숨김
            binding.ivPoster.loadPoster(movie?.posterPath)
            binding.ratingView.isVisible = movie != null
            movie?.let { binding.ratingView.setRating(it.voteAverage) }

            binding.cardBoxOffice.contentDescription = context.getString(
                R.string.cd_box_office_item,
                boxOffice.rank,
                boxOffice.movieName,
                NumberFormat.getNumberInstance(Locale.KOREA).format(boxOffice.audienceCount)
            )
            binding.cardBoxOffice.setOnClickListener { onItemClick(item) }
        }
    }

    private object BoxOfficeDiffCallback : DiffUtil.ItemCallback<BoxOfficeMovie>() {
        override fun areItemsTheSame(oldItem: BoxOfficeMovie, newItem: BoxOfficeMovie): Boolean =
            oldItem.boxOffice.movieCode == newItem.boxOffice.movieCode

        override fun areContentsTheSame(oldItem: BoxOfficeMovie, newItem: BoxOfficeMovie): Boolean =
            oldItem == newItem
    }
}
