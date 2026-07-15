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

class ReviewAdapter(
    private val onHelpfulClick: (Review) -> Unit
) : ListAdapter<Review, ReviewAdapter.ReviewViewHolder>(DIFF_CALLBACK) {

    // 확장된 리뷰 ID 집합 — RecyclerView 재활용 시에도 상태가 보존된다
    private val expandedIds = mutableSetOf<String>()

    // 도움이 됨으로 표시된 리뷰 ID 집합 — 어댑터는 이 값을 직접 소유하지 않고
    // setHelpfulIds()로 주입받은 값만 그린다 (낙관적 mutate 금지)
    private var helpfulIds: Set<String> = emptySet()

    // ViewModel의 helpfulReviewIds를 주입받아 아이콘 표시를 갱신한다 (변경된 항목만 notifyItemChanged)
    fun setHelpfulIds(ids: Set<String>) {
        val previous = helpfulIds
        if (previous == ids) return
        helpfulIds = ids
        val changedIds = (previous - ids) + (ids - previous)
        currentList.forEachIndexed { index, review ->
            if (review.id in changedIds) notifyItemChanged(index)
        }
    }

    // 리뷰 아이템 ViewHolder 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReviewViewHolder(binding, onHelpfulClick)
    }

    // 리뷰 데이터를 ViewHolder에 바인딩
    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it, expandedIds, it.id in helpfulIds) }
    }

    class ReviewViewHolder(
        private val binding: ItemReviewBinding,
        private val onHelpfulClick: (Review) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        // rebind 시 이전 접근성 액션이 누적되지 않도록 ID 추적
        private var expandCollapseActionId: Int = -1

        // 접근성: 확장/축소 상태 설명 갱신
        private fun updateStateDescription(isExpanded: Boolean) {
            val desc = itemView.context.getString(
                if (isExpanded) R.string.cd_review_expanded else R.string.cd_review_collapsed
            )
            ViewCompat.setStateDescription(binding.root, desc)
        }

        // 도움이 됨 아이콘/설명 갱신 (어댑터가 주입한 상태만 반영, 자체 mutate 없음)
        private fun updateHelpfulIcon(isHelpful: Boolean) {
            binding.btnHelpful.setImageResource(
                if (isHelpful) R.drawable.ic_thumb_up else R.drawable.ic_thumb_up_outline
            )
            binding.btnHelpful.contentDescription = itemView.context.getString(
                if (isHelpful) R.string.cd_review_helpful_on else R.string.cd_review_helpful_off
            )
        }

        // 리뷰 작성자, 평점, 내용, 날짜, 도움이 됨 상태 바인딩 및 클릭 리스너 설정
        fun bind(review: Review, expandedIds: MutableSet<String>, isHelpful: Boolean) {
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
            // 이전 액션을 먼저 제거하여 rebind 시 액션 누적 방지
            if (expandCollapseActionId != -1) {
                ViewCompat.removeAccessibilityAction(binding.root, expandCollapseActionId)
            }
            expandCollapseActionId = ViewCompat.addAccessibilityAction(binding.root, actionLabel) { _, _ ->
                binding.root.performClick()
                true
            }

            binding.tvDate.text = review.createdAt.take(10)
            val context = binding.root.context
            val ratingText = review.rating?.let { context.getString(R.string.cd_review_rating, it) } ?: ""
            val dateText = review.createdAt.take(10)
            binding.root.contentDescription =
                context.getString(R.string.cd_review_item, review.author, ratingText, dateText)

            binding.root.setOnClickListener {
                val nowExpanded = review.id in expandedIds
                if (nowExpanded) expandedIds.remove(review.id) else expandedIds.add(review.id)
                binding.tvContent.maxLines = if (!nowExpanded) Int.MAX_VALUE else COLLAPSED_MAX_LINES
                updateStateDescription(!nowExpanded)
            }

            // 도움이 됨 토글은 행 전체 확장/축소 클릭과 별도 클릭 타깃 — 어댑터는 콜백만 호출하고 낙관적 mutate는 하지 않는다
            updateHelpfulIcon(isHelpful)
            binding.btnHelpful.setOnClickListener { onHelpfulClick(review) }
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
