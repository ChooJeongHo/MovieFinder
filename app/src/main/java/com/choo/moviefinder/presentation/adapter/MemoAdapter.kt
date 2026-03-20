package com.choo.moviefinder.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.choo.moviefinder.databinding.ItemMemoBinding
import com.choo.moviefinder.domain.model.Memo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    private class MemoDiffCallback : DiffUtil.ItemCallback<Memo>() {
        override fun areItemsTheSame(oldItem: Memo, newItem: Memo) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Memo, newItem: Memo) = oldItem == newItem
    }
}
