package com.choo.moviefinder.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.choo.moviefinder.databinding.ItemReviewBinding
import com.choo.moviefinder.domain.model.Review
import java.util.Locale

class ReviewAdapter : ListAdapter<Review, ReviewAdapter.ReviewViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReviewViewHolder(
        private val binding: ItemReviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var isExpanded = false

        fun bind(review: Review) {
            binding.tvAuthor.text = review.author

            if (review.rating != null) {
                binding.tvRating.text = String.format(Locale.US, "★ %.1f", review.rating)
                binding.tvRating.isVisible = true
            } else {
                binding.tvRating.isVisible = false
            }

            binding.tvContent.text = review.content
            binding.tvContent.maxLines = 4
            isExpanded = false

            binding.tvDate.text = review.createdAt.take(10)

            binding.root.setOnClickListener {
                isExpanded = !isExpanded
                binding.tvContent.maxLines = if (isExpanded) Integer.MAX_VALUE else 4
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Review>() {
            override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean =
                oldItem == newItem
        }
    }
}
