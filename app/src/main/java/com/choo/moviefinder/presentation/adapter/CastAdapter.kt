package com.choo.moviefinder.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.dispose
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.choo.moviefinder.core.util.ImageUrlProvider
import com.choo.moviefinder.databinding.ItemCastBinding
import com.choo.moviefinder.domain.model.Cast

class CastAdapter(
    private val onCastClick: (Int) -> Unit = {}
) : ListAdapter<Cast, CastAdapter.CastViewHolder>(DIFF_CALLBACK) {

    // 출연진 아이템 ViewHolder 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CastViewHolder {
        val binding = ItemCastBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CastViewHolder(binding)
    }

    // 출연진 데이터를 ViewHolder에 바인딩
    override fun onBindViewHolder(holder: CastViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    // 재활용 시 Coil 프로필 이미지 로드 취소
    override fun onViewRecycled(holder: CastViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.ivProfile.dispose()
    }

    inner class CastViewHolder(
        val binding: ItemCastBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // 출연진 이름, 배역, 프로필 이미지 바인딩
        fun bind(cast: Cast) {
            binding.tvName.text = cast.name
            binding.tvCharacter.text = cast.character

            binding.ivProfile.load(ImageUrlProvider.profileUrl(cast.profilePath)) {
                crossfade(true)
                placeholder(com.choo.moviefinder.R.drawable.bg_circle)
                error(com.choo.moviefinder.R.drawable.bg_circle)
            }

            binding.root.setOnClickListener {
                onCastClick(cast.id)
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
