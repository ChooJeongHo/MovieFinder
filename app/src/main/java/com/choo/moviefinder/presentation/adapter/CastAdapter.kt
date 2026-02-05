package com.choo.moviefinder.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.choo.moviefinder.core.util.ImageUrlProvider
import com.choo.moviefinder.databinding.ItemCastBinding
import com.choo.moviefinder.domain.model.Cast

class CastAdapter : ListAdapter<Cast, CastAdapter.CastViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CastViewHolder {
        val binding = ItemCastBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CastViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CastViewHolder(
        private val binding: ItemCastBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cast: Cast) {
            binding.tvName.text = cast.name
            binding.tvCharacter.text = cast.character

            binding.ivProfile.load(ImageUrlProvider.profileUrl(cast.profilePath)) {
                crossfade(true)
                placeholder(com.choo.moviefinder.R.drawable.bg_circle)
                error(com.choo.moviefinder.R.drawable.bg_circle)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Cast>() {
            override fun areItemsTheSame(oldItem: Cast, newItem: Cast): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Cast, newItem: Cast): Boolean =
                oldItem == newItem
        }
    }
}
