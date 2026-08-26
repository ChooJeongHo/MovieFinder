package com.choo.moviefinder.presentation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.dispose
import com.choo.moviefinder.R
import com.choo.moviefinder.databinding.ItemBoxOfficeBinding
import com.choo.moviefinder.domain.model.BoxOffice
import com.choo.moviefinder.domain.model.BoxOfficeMovie
import com.choo.moviefinder.domain.model.KoreanRating
import com.choo.moviefinder.domain.model.KoreanRatingGrade
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

            val koreanRating = item.koreanRating
            binding.tvKoreanRating.isVisible = koreanRating != null
            koreanRating?.let { binding.tvKoreanRating.text = ratingBadgeLabel(context, it) }

            binding.cardBoxOffice.contentDescription = listOfNotNull(
                context.getString(
                    R.string.cd_box_office_item,
                    boxOffice.rank,
                    boxOffice.movieName,
                    NumberFormat.getNumberInstance(Locale.KOREA).format(boxOffice.audienceCount),
                    rankChangeDescription(context, boxOffice)
                ),
                koreanRating?.let { context.getString(R.string.cd_korean_rating, it.gradeName) }
            ).joinToString(", ")
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

// tv_rank_change 배지(▲▼NEW)는 importantForAccessibility="no"라 TalkBack이 직접 읽지 않으므로,
// 같은 정보를 cardBoxOffice의 contentDescription에 자연어 문장으로 포함시킨다.
private fun rankChangeDescription(context: Context, boxOffice: BoxOffice): String = when {
    boxOffice.isNewEntry -> context.getString(R.string.cd_box_office_rank_new)
    boxOffice.rankChange > 0 -> context.getString(R.string.cd_box_office_rank_up, boxOffice.rankChange)
    boxOffice.rankChange < 0 -> context.getString(R.string.cd_box_office_rank_down, -boxOffice.rankChange)
    else -> context.getString(R.string.cd_box_office_rank_same)
}

// 좁은 카드(movie_card_horizontal_width)에 맞는 축약 배지 라벨. 알려진 4종 외 등급(매핑 실패)은
// KMRB 원문 gradeName으로 폴백해 정보 손실 없이 표시한다.
private fun ratingBadgeLabel(context: Context, koreanRating: KoreanRating): String =
    when (KoreanRatingGrade.fromApiGradeName(koreanRating.gradeName)) {
        KoreanRatingGrade.ALL_AGES -> context.getString(R.string.box_office_rating_badge_all_ages)
        KoreanRatingGrade.TWELVE_AND_UP -> context.getString(R.string.box_office_rating_badge_12)
        KoreanRatingGrade.FIFTEEN_AND_UP -> context.getString(R.string.box_office_rating_badge_15)
        KoreanRatingGrade.RESTRICTED -> context.getString(R.string.box_office_rating_badge_restricted)
        null -> koreanRating.gradeName
    }
