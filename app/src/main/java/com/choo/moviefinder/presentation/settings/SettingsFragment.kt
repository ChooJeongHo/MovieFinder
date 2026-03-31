package com.choo.moviefinder.presentation.settings

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil3.imageLoader
import com.choo.moviefinder.BuildConfig
import com.choo.moviefinder.R
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.core.util.FileLoggingTree
import com.choo.moviefinder.databinding.FragmentSettingsBinding
import com.choo.moviefinder.domain.model.ThemeMode
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
@Suppress("TooManyFunctions")
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()
    private var activeDialog: Dialog? = null

    // 내보내기: JSON 파일 저장 위치 선택 후 내용을 기록
    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            try {
                viewModel.pendingExportJson?.let { json ->
                    requireContext().contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray())
                    }
                    viewModel.pendingExportJson = null
                    Snackbar.make(binding.root, R.string.export_success, Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, R.string.export_error, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    // 가져오기: JSON 파일 선택 후 내용을 읽어 ViewModel에 전달
    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            try {
                val json = requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes().toString(Charsets.UTF_8)
                } ?: return@launch
                showImportConfirmDialog(json)
            } catch (e: Exception) {
                Snackbar.make(binding.root, R.string.import_error, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    // 설정 화면 레이아웃을 인플레이트하고 바인딩 초기화
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // 뷰 생성 후 설정 항목 클릭 리스너, 테마/언어 관찰 등 UI 초기화
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupAppInfo()
        updateLanguageDisplay()
        observeSettings()
        observeEvents()
    }

    // 테마, 언어, 통계, 시청 목표, 캐시 삭제, 시청기록 삭제, 내보내기, 가져오기, 로그 공유 항목 클릭 리스너 등록
    private fun setupClickListeners() {
        binding.itemTheme.setOnClickListener { showThemeDialog() }
        binding.itemLanguage.setOnClickListener { showLanguageDialog() }
        binding.itemStats.setOnClickListener { navigateToStats() }
        binding.itemWatchGoal.setOnClickListener { showWatchGoalDialog() }
        binding.itemClearCache.setOnClickListener { showClearCacheDialog() }
        binding.itemClearWatchHistory.setOnClickListener { showClearWatchHistoryDialog() }
        binding.itemExportData.setOnClickListener { viewModel.exportData() }
        binding.itemImportData.setOnClickListener {
            openDocumentLauncher.launch(arrayOf("application/json"))
        }
        if (BuildConfig.DEBUG) {
            binding.itemShareLogs.isVisible = true
            binding.dividerShareLogs.isVisible = true
            binding.itemShareLogs.setOnClickListener { shareDebugLogs() }
        }
    }

    // 디버그 로그 파일을 FileProvider를 통해 공유한다
    private fun shareDebugLogs() {
        val logFile = FileLoggingTree.getLogFile(requireContext())
        if (!logFile.exists()) {
            Snackbar.make(binding.root, R.string.share_debug_logs_empty, Snackbar.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            logFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_debug_logs)))
    }

    // 시청 통계 화면으로 이동
    private fun navigateToStats() {
        if (findNavController().currentDestination?.id == R.id.settingsFragment) {
            findNavController().navigate(R.id.action_settings_to_stats)
        }
    }

    // 앱 버전 정보 텍스트 설정
    private fun setupAppInfo() {
        binding.tvAppVersion.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)
    }

    // 테마 모드와 시청 목표 Flow를 수집하여 표시 텍스트 갱신
    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentThemeMode.collect { mode ->
                    binding.tvThemeValue.text = when (mode) {
                        ThemeMode.LIGHT -> getString(R.string.theme_light)
                        ThemeMode.DARK -> getString(R.string.theme_dark)
                        ThemeMode.SYSTEM -> getString(R.string.theme_system)
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.monthlyWatchGoal.collect { goal ->
                    binding.tvWatchGoalValue.text = if (goal == 0) {
                        getString(R.string.settings_watch_goal_not_set)
                    } else {
                        getString(R.string.stats_count_format, goal)
                    }
                }
            }
        }
    }

    // 테마 선택 다이얼로그 표시 (라이트/다크/시스템)
    private fun showThemeDialog() {
        val themes = arrayOf(
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
            getString(R.string.theme_system)
        )
        val currentIndex = viewModel.currentThemeMode.value.ordinal

        activeDialog?.dismiss()
        activeDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_theme)
            .setSingleChoiceItems(themes, currentIndex) { dialog, which ->
                val selectedMode = ThemeMode.entries[which]
                viewModel.setThemeMode(selectedMode)
                dialog.dismiss()
            }
            .show()
    }

    // 언어 선택 다이얼로그 표시 (시스템/한국어/영어)
    private fun showLanguageDialog() {
        val languageLabels = arrayOf(
            getString(R.string.language_system),
            getString(R.string.language_korean),
            getString(R.string.language_english)
        )
        val languageTags = arrayOf("", "ko", "en")
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val currentTag = if (currentLocales.isEmpty) "" else currentLocales.toLanguageTags()
        val currentIndex = languageTags.indexOfFirst {
            it.equals(currentTag, ignoreCase = true)
        }.coerceAtLeast(0)

        activeDialog?.dismiss()
        activeDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_language)
            .setSingleChoiceItems(languageLabels, currentIndex) { dialog, which ->
                val tag = languageTags[which]
                val localeList = if (tag.isEmpty()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(tag)
                }
                AppCompatDelegate.setApplicationLocales(localeList)
                dialog.dismiss()
            }
            .show()
    }

    // 현재 설정된 앱 언어를 텍스트로 표시
    private fun updateLanguageDisplay() {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        binding.tvLanguageValue.text = if (currentLocales.isEmpty) {
            getString(R.string.language_system)
        } else {
            when (currentLocales.toLanguageTags().lowercase()) {
                "ko" -> getString(R.string.language_korean)
                "en" -> getString(R.string.language_english)
                else -> getString(R.string.language_system)
            }
        }
    }

    // 이번 달 시청 목표 편수 설정 다이얼로그 표시 (NumberPicker 0~100)
    private fun showWatchGoalDialog() {
        val numberPicker = android.widget.NumberPicker(requireContext()).apply {
            minValue = 0
            maxValue = 100
            value = viewModel.monthlyWatchGoal.value
            wrapSelectorWheel = false
        }
        activeDialog?.dismiss()
        activeDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_watch_goal)
            .setMessage(R.string.settings_watch_goal_dialog_message)
            .setView(numberPicker)
            .setPositiveButton(R.string.confirm) { _, _ ->
                viewModel.setMonthlyWatchGoal(numberPicker.value)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // 캐시 삭제 확인 다이얼로그 표시
    private fun showClearCacheDialog() {
        activeDialog?.dismiss()
        activeDialog = MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.confirm_clear_cache)
            .setPositiveButton(R.string.confirm) { _, _ ->
                clearCache()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // Coil 메모리 및 디스크 이미지 캐시 삭제
    private fun clearCache() {
        val imageLoader = requireContext().imageLoader
        try {
            imageLoader.memoryCache?.clear()
        } finally {
            imageLoader.diskCache?.clear()
        }
        Snackbar.make(binding.root, R.string.cache_cleared, Snackbar.LENGTH_SHORT).show()
    }

    // 시청 기록 삭제 확인 다이얼로그 표시
    private fun showClearWatchHistoryDialog() {
        activeDialog?.dismiss()
        activeDialog = MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.confirm_clear_watch_history)
            .setPositiveButton(R.string.confirm) { _, _ ->
                viewModel.clearWatchHistory()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // 시청 기록 삭제 성공/에러/내보내기/가져오기 이벤트를 수집하여 Snackbar 표시
    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.watchHistoryCleared.collect {
                    Snackbar.make(binding.root, R.string.watch_history_cleared, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.snackbarEvent.collect { errorType ->
                    val message = ErrorMessageProvider.getMessage(requireContext(), errorType)
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.exportedJson.collect { json ->
                    viewModel.pendingExportJson = json
                    createDocumentLauncher.launch("moviefinder_backup.json")
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.importSuccess.collect {
                    Snackbar.make(binding.root, R.string.import_success, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isImporting.collect { importing ->
                    binding.itemImportData.isEnabled = !importing
                    binding.itemImportData.alpha = if (importing) 0.5f else 1.0f
                }
            }
        }
    }

    // 가져오기 확인 다이얼로그 표시
    private fun showImportConfirmDialog(json: String) {
        activeDialog?.dismiss()
        activeDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.import_confirm_title)
            .setMessage(R.string.import_confirm_message)
            .setPositiveButton(R.string.confirm) { _, _ ->
                viewModel.importData(json)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // 다이얼로그 dismiss 및 바인딩 null 처리
    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
    }
}
