package com.choo.moviefinder.presentation.reminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.choo.moviefinder.R
import com.choo.moviefinder.databinding.FragmentReminderHistoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReminderHistoryFragment : Fragment() {

    private var _binding: FragmentReminderHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReminderHistoryViewModel by viewModels()
    private lateinit var adapter: ReminderAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReminderHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        observeViewModelFlows()
    }

    // 툴바 뒤로가기 버튼 설정
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    // RecyclerView 및 어댑터 초기화
    private fun setupRecyclerView() {
        adapter = ReminderAdapter { reminder ->
            showCancelConfirmDialog(reminder.movieId, reminder.movieTitle)
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.itemAnimator = null
    }

    // 알림 취소 확인 다이얼로그 표시
    private fun showCancelConfirmDialog(movieId: Int, movieTitle: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reminder_cancel_confirm_title)
            .setMessage(getString(R.string.reminder_cancel_confirm_message, movieTitle))
            .setPositiveButton(R.string.confirm) { _, _ ->
                viewModel.cancelReminder(movieId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // 모든 ViewModel Flow를 단일 repeatOnLifecycle 블록에서 병렬 수집
    private fun observeViewModelFlows() {
        binding.shimmerView.shimmerLayout.startShimmer()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    try {
                        viewModel.reminders.collect { reminders ->
                            val shimmer = binding.shimmerView.shimmerLayout
                            shimmer.stopShimmer()
                            shimmer.isVisible = false
                            binding.errorView.layoutError.isVisible = false
                            adapter.submitList(reminders)
                            binding.tvEmpty.isVisible = reminders.isEmpty()
                            binding.recyclerView.isVisible = reminders.isNotEmpty()
                        }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        binding.shimmerView.shimmerLayout.stopShimmer()
                        binding.shimmerView.shimmerLayout.isVisible = false
                        binding.errorView.layoutError.isVisible = true
                    }
                }
                launch {
                    viewModel.cancelledEvent.collect {
                        Snackbar.make(
                            binding.root,
                            R.string.reminder_cancelled,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
                launch {
                    viewModel.errorEvent.collect {
                        Snackbar.make(
                            binding.root,
                            R.string.reminder_cancel_failed,
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.shimmerView.shimmerLayout.stopShimmer()
        _binding = null
    }
}
