package com.choo.moviefinder.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.ViewCompat
import com.choo.moviefinder.R
import com.choo.moviefinder.databinding.ItemReviewBinding
import com.choo.moviefinder.domain.model.Review
import java.util.Locale

class ReviewAdapter : ListAdapter<Review, ReviewAdapter.ReviewViewHolder>(DIFF_CALLBACK) {

    // 확장된 리뷰 ID 집합 — RecyclerView 재활용 시에도 상태가 보존된다
    private val expandedIds = mutableSetOf<String>()

    // 리뷰 아이템 ViewHolder 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReviewViewHolder(binding)
    }

    // 리뷰 데이터를 ViewHolder에 바인딩
    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it, expandedIds) }
    }

    class ReviewViewHolder(
        private val binding: ItemReviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // 접근성: 확장/축소 상태 설명 갱신
        private fun updateStateDescription(isExpanded: Boolean) {
            val desc = itemView.context.getString(
                if (isExpanded) R.string.cd_review_expanded else R.string.cd_review_collapsed
            )
            ViewCompat.setStateDescription(binding.root, desc)
        }

        // 리뷰 작성자, 평점, 내용, 날짜 바인딩 및 클릭 확장/축소 설정
        fun bind(review: Review, expandedIds: MutableSet<String>) {
            binding.tvAuthor.text = review.author

            if (review.rating != null) {
                binding.tvRating.text = String.format(Locale.US, "★ %.1f", review.rating)
                binding.tvRating.isVisible = true
            } else {
                binding.tvRating.isVisible = false
            }

            binding.tvContent.text = review.content
            val isExpanded = review.id in expandedIds
            binding.tvContent.maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_MAX_LINES
            updateStateDescription(isExpanded)
            val actionLabel = binding.root.context.getString(
                if (isExpanded) R.string.cd_review_collapse else R.string.cd_review_expand
            )
            ViewCompat.addAccessibilityAction(binding.root, actionLabel) { _, _ ->
                binding.root.performClick()
                true
            }

            binding.tvDate.text = review.createdAt.take(10)
            binding.root.contentDescription = review.author

            binding.root.setOnClickListener {
                val nowExpanded = review.id in expandedIds
                if (nowExpanded) expandedIds.remove(review.id) else expandedIds.add(review.id)
                binding.tvContent.maxLines = if (!nowExpanded) Int.MAX_VALUE else COLLAPSED_MAX_LINES
                updateStateDescription(!nowExpanded)
            }
        }
    }

    companion object {
        private const val COLLAPSED_MAX_LINES = 4
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Review>() {
            override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean =
                oldItem == newItem
        }
    }
}
