package com.choo.moviefinder.presentation.reminder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.choo.moviefinder.R
import com.choo.moviefinder.databinding.ItemReminderBinding
import com.choo.moviefinder.domain.model.ScheduledReminder

class ReminderAdapter(
    private val onCancelClick: (ScheduledReminder) -> Unit
) : ListAdapter<ScheduledReminder, ReminderAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReminderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemReminderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(reminder: ScheduledReminder) {
            binding.tvMovieTitle.text = reminder.movieTitle
            binding.tvReleaseDate.text = reminder.releaseDate
            binding.btnCancelReminder.contentDescription =
                binding.root.context.getString(R.string.cd_cancel_reminder_for, reminder.movieTitle)
            binding.btnCancelReminder.setOnClickListener {
                onCancelClick(reminder)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ScheduledReminder>() {
        override fun areItemsTheSame(oldItem: ScheduledReminder, newItem: ScheduledReminder) =
            oldItem.movieId == newItem.movieId

        override fun areContentsTheSame(oldItem: ScheduledReminder, newItem: ScheduledReminder) =
            oldItem == newItem
    }
}
