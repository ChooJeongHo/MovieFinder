package com.choo.moviefinder.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.databinding.ItemLoadStateBinding

class MovieLoadStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<MovieLoadStateAdapter.LoadStateViewHolder>() {

    // 로딩/에러 상태 footer ViewHolder 생성
    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadStateViewHolder {
        val binding = ItemLoadStateBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LoadStateViewHolder(binding)
    }

    // 로딩 상태에 따라 ProgressBar 또는 에러 메시지 표시
    override fun onBindViewHolder(holder: LoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    inner class LoadStateViewHolder(
        private val binding: ItemLoadStateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnRetry.setOnClickListener { retry() }
        }

        // LoadState에 따라 로딩/에러 UI 전환
        fun bind(loadState: LoadState) {
            binding.progressBar.isVisible = loadState is LoadState.Loading
            binding.tvErrorMessage.isVisible = loadState is LoadState.Error
            binding.btnRetry.isVisible = loadState is LoadState.Error

            if (loadState is LoadState.Error) {
                binding.tvErrorMessage.text =
                    ErrorMessageProvider.getMessage(itemView.context, loadState.error)
            }
        }
    }
}
