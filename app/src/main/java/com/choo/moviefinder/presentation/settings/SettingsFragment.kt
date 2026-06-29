package com.choo.moviefinder.presentation.settings

import android.app.Dialog
import android.content.Intent
import android.net.Uri
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
@Suppress("TooManyFunctions")
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()
    private var activeDialog: Dialog? = null

    // 내보내기: JSON 파일 저장 위치 선택 후 내용 기록
    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        viewLifecycleOwner.lifecycleScope.launch {
            val b = _binding ?: return@launch
            try {
                viewModel.pendingExportJson?.let { json ->
                    withContext(Dispatchers.IO) {
                        requireContext().contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(json.toByteArray())
                        }
                    }
                    viewModel.pendingExportJson = null
                    Snackbar.make(b.root, R.string.export_success, Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Snackbar.make(b.root, R.string.export_error, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    // 가져오기: JSON 파일 선택 후 ViewModel에 전달
    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        viewLifecycleOwner.lifecycleScope.launch {
            val b = _binding ?: return@launch
            try {
                val maxImportBytes = 10L * 1024 * 1024 // 10MB
                val json = withContext(Dispatchers.IO) {
                    val fileSize = requireContext().contentResolver
                        .openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
                    if (fileSize > maxImportBytes) {
                        null
                    } else {
                        requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                            stream.readBytes().toString(Charsets.UTF_8)
                        }
                    }
                } ?: run {
                    Snackbar.make(b.root, R.string.import_error, Snackbar.LENGTH_SHORT).show()
                    return@launch
                }
                showImportConfirmDialog(json)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Snackbar.make(b.root, R.string.import_error, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // 클릭 리스너, 앱 버전, 언어 표시, Flow 수집 초기화
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupAppInfo()
        updateLanguageDisplay()
        observeViewModelFlows()
    }

    private fun setupClickListeners() {
        binding.itemTheme.setOnClickListener { showThemeDialog() }
        binding.itemLanguage.setOnClickListener { showLanguageDialog() }
        binding.itemStats.setOnClickListener { navigateToStats() }
        binding.itemWatchGoal.setOnClickListener { showWatchGoalDialog() }
        binding.itemClearCache.setOnClickListener { showClearCacheDialog() }
        binding.itemClearWatchHistory.setOnClickListener { showClearWatchHistoryDialog() }
        binding.itemReminderHistory.setOnClickListener { navigateToReminderHistory() }
        binding.itemExportData.setOnClickListener { viewModel.exportData() }
        binding.itemImportData.setOnClickListener {
            openDocumentLauncher.launch(arrayOf("application/json"))
        }
        binding.btnTmdbConnect.setOnClickListener { viewModel.startTmdbAuth() }
        binding.btnTmdbDisconnect.setOnClickListener { viewModel.disconnectTmdb() }
        binding.btnTmdbSync.setOnClickListener { viewModel.syncTmdbAccount() }
        if (BuildConfig.DEBUG) {
            binding.itemShareLogs.isVisible = true
            binding.dividerShareLogs.isVisible = true
            binding.itemShareLogs.setOnClickListener { shareDebugLogs() }
        }
    }

    // FileProvider를 통해 로그 파일 공유
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

    private fun navigateToStats() {
        if (findNavController().currentDestination?.id == R.id.settingsFragment) {
            findNavController().navigate(R.id.action_settings_to_stats)
        }
    }

    private fun navigateToReminderHistory() {
        if (findNavController().currentDestination?.id == R.id.settingsFragment) {
            findNavController().navigate(R.id.action_settings_to_reminder_history)
        }
    }

    private fun setupAppInfo() {
        binding.tvAppVersion.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)
    }

    // 단일 repeatOnLifecycle 블록에서 병렬 수집
    private fun observeViewModelFlows() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentThemeMode.collect { mode ->
                        binding.tvThemeValue.text = when (mode) {
                            ThemeMode.LIGHT -> getString(R.string.theme_light)
                            ThemeMode.DARK -> getString(R.string.theme_dark)
                            ThemeMode.SYSTEM -> getString(R.string.theme_system)
                        }
                    }
                }
                launch {
                    viewModel.monthlyWatchGoal.collect { goal ->
                        binding.tvWatchGoalValue.text = if (goal == 0) {
                            getString(R.string.settings_watch_goal_not_set)
                        } else {
                            resources.getQuantityString(R.plurals.stats_count_format, goal, goal)
                        }
                    }
                }
                launch {
                    viewModel.watchHistoryCleared.collect {
                        Snackbar.make(binding.root, R.string.watch_history_cleared, Snackbar.LENGTH_SHORT).show()
                    }
                }
                launch {
                    viewModel.snackbarEvent.collect { errorType ->
                        val message = ErrorMessageProvider.getMessage(requireContext(), errorType)
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                    }
                }
                launch {
                    viewModel.exportedJson.collect { json ->
                        viewModel.pendingExportJson = json
                        if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            createDocumentLauncher.launch("moviefinder_backup.json")
                        }
                    }
                }
                launch {
                    viewModel.importSuccess.collect { count ->
                        val message = if (count == 0) R.string.import_empty else R.string.import_success
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                    }
                }
                launch {
                    viewModel.isImporting.collect { importing ->
                        binding.itemImportData.isEnabled = !importing
                        binding.itemImportData.alpha = if (importing) 0.5f else 1.0f
                    }
                }
                observeTmdbFlows(this)
            }
        }
    }

    // TMDB 연결 상태·인증 URL·동기화 결과·동기화 진행 상태 수집
    private fun observeTmdbFlows(scope: CoroutineScope) {
        scope.launch {
            viewModel.tmdbAccessToken.collect { token ->
                val connected = token != null
                binding.tvTmdbStatus.text = if (connected) {
                    getString(R.string.tmdb_connected)
                } else {
                    getString(R.string.tmdb_not_connected)
                }
                binding.btnTmdbConnect.isVisible = !connected
                binding.btnTmdbDisconnect.isVisible = connected
                binding.btnTmdbSync.isVisible = connected
            }
        }
        scope.launch {
            viewModel.openTmdbAuth.collect { url ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: android.content.ActivityNotFoundException) {
                    Snackbar.make(binding.root, R.string.tmdb_no_browser, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        scope.launch {
            viewModel.disconnectSuccess.collect {
                Snackbar.make(binding.root, R.string.tmdb_disconnected, Snackbar.LENGTH_SHORT).show()
            }
        }
        scope.launch {
            viewModel.syncResult.collect { result ->
                val message = when (result) {
                    is SyncResult.Success -> getString(
                        R.string.tmdb_sync_success,
                        result.favoritesAdded,
                        result.watchlistAdded
                    )
                    is SyncResult.Failed -> getString(R.string.tmdb_sync_failed, result.message)
                }
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            }
        }
        scope.launch {
            viewModel.isSyncing.collect { syncing ->
                binding.btnTmdbSync.isEnabled = !syncing
                binding.btnTmdbSync.text = if (syncing) {
                    getString(R.string.tmdb_syncing)
                } else {
                    getString(R.string.tmdb_sync)
                }
            }
        }
    }

    // (라이트/다크/시스템)
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

    // (시스템/한국어/영어)
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

    // NumberPicker 0~100
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

    // Coil 메모리+디스크 캐시 삭제, 디스크 I/O는 IO Dispatcher에서
    private fun clearCache() {
        val imageLoader = requireContext().imageLoader
        imageLoader.memoryCache?.clear()
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                imageLoader.diskCache?.clear()
            }
            Snackbar.make(binding.root, R.string.cache_cleared, Snackbar.LENGTH_SHORT).show()
        }
    }

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

    // activeDialog dismiss + 바인딩 null
    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
    }
}
