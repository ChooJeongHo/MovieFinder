package com.choo.moviefinder.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.choo.moviefinder.databinding.ItemRecentSearchBinding

class RecentSearchAdapter(
    private val onItemClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : ListAdapter<String, RecentSearchAdapter.RecentSearchViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentSearchViewHolder {
        val binding = ItemRecentSearchBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RecentSearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentSearchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecentSearchViewHolder(
        private val binding: ItemRecentSearchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(query: String) {
            binding.tvQuery.text = query

            binding.root.setOnClickListener {
                onItemClick(query)
            }

            binding.ivDelete.setOnClickListener {
                onDeleteClick(query)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem
        }
    }
}
