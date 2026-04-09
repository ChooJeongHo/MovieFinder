@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.choo.moviefinder.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.choo.moviefinder.databinding.ItemMemoBinding
import com.choo.moviefinder.domain.model.Memo
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class MemoAdapter(
    private val onEditClick: (Memo) -> Unit,
    private val onDeleteClick: (Memo) -> Unit
) : ListAdapter<Memo, MemoAdapter.MemoViewHolder>(MemoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        val binding = ItemMemoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    inner class MemoViewHolder(
        private val binding: ItemMemoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(memo: Memo) {
            binding.tvMemoContent.text = memo.content
            binding.tvMemoDate.text = formatDate(memo.updatedAt)
            binding.btnEditMemo.setOnClickListener { onEditClick(memo) }
            binding.btnDeleteMemo.setOnClickListener { onDeleteClick(memo) }
        }

        private fun formatDate(timestamp: Long): String {
            val instant = Instant.fromEpochMilliseconds(timestamp)
            val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            return "%04d.%02d.%02d %02d:%02d".format(
                dateTime.year, dateTime.monthNumber, dateTime.dayOfMonth,
                dateTime.hour, dateTime.minute
            )
        }
    }

    private class MemoDiffCallback : DiffUtil.ItemCallback<Memo>() {
        override fun areItemsTheSame(oldItem: Memo, newItem: Memo) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Memo, newItem: Memo) = oldItem == newItem
    }
}
