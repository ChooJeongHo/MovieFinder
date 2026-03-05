package com.choo.moviefinder.presentation.settings

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.imageLoader
import com.choo.moviefinder.BuildConfig
import com.choo.moviefinder.R
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.databinding.FragmentSettingsBinding
import com.choo.moviefinder.domain.model.ThemeMode
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()
    private var activeDialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupAppInfo()
        updateLanguageDisplay()
        observeTheme()
        observeEvents()
    }

    private fun setupClickListeners() {
        binding.itemTheme.setOnClickListener { showThemeDialog() }
        binding.itemLanguage.setOnClickListener { showLanguageDialog() }
        binding.itemClearCache.setOnClickListener { showClearCacheDialog() }
        binding.itemClearWatchHistory.setOnClickListener { showClearWatchHistoryDialog() }
    }

    private fun setupAppInfo() {
        binding.tvAppVersion.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)
    }

    private fun observeTheme() {
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
    }

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

    private fun clearCache() {
        requireContext().imageLoader.memoryCache?.clear()
        requireContext().imageLoader.diskCache?.clear()
        Snackbar.make(binding.root, R.string.cache_cleared, Snackbar.LENGTH_SHORT).show()
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
    }
}
