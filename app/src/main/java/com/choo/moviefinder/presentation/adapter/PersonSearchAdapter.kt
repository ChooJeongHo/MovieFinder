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
import coil3.size.ViewSizeResolver
import com.choo.moviefinder.R
import com.choo.moviefinder.core.util.ImageUrlProvider
import com.choo.moviefinder.databinding.ItemPersonSearchBinding
import com.choo.moviefinder.domain.model.PersonSearchItem

class PersonSearchAdapter(
    private val onPersonClick: (Int) -> Unit
) : ListAdapter<PersonSearchItem, PersonSearchAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPersonSearchBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onPersonClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // 재활용 시 Coil 이미지 로드 취소
    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.ivProfile.dispose()
    }

    class ViewHolder(
        val binding: ItemPersonSearchBinding,
        private val onPersonClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PersonSearchItem) {
            binding.tvName.text = item.name

            val context = binding.root.context
            if (item.knownForDepartment.isNotEmpty()) {
                binding.tvKnownForDepartment.text =
                    context.getString(R.string.person_known_for_format, item.knownForDepartment)
            } else {
                binding.tvKnownForDepartment.text = ""
            }

            binding.tvKnownForTitles.text = item.knownForTitles

            val profileUrl = ImageUrlProvider.profileUrl(item.profilePath)
            if (profileUrl != null) {
                binding.ivProfile.load(profileUrl) {
                    crossfade(true)
                    placeholder(R.drawable.bg_circle)
                    error(R.drawable.bg_circle)
                    size(ViewSizeResolver(binding.ivProfile))
                }
            } else {
                binding.ivProfile.setImageResource(R.drawable.bg_circle)
            }

            binding.root.setOnClickListener {
                onPersonClick(item.id)
            }

            binding.root.contentDescription = item.name
        }
    }

    private companion object DiffCallback : DiffUtil.ItemCallback<PersonSearchItem>() {
        override fun areItemsTheSame(oldItem: PersonSearchItem, newItem: PersonSearchItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PersonSearchItem, newItem: PersonSearchItem) =
            oldItem == newItem
    }
}
