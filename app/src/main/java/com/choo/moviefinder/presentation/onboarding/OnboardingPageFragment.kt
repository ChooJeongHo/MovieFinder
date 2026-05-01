package com.choo.moviefinder.presentation.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment

class OnboardingPageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            MaterialTheme {
                OnboardingPage(
                    icon = requireArguments().getInt(ARG_ICON),
                    title = requireArguments().getString(ARG_TITLE, ""),
                    description = requireArguments().getString(ARG_DESC, ""),
                )
            }
        }
    }

    companion object {
        private const val ARG_ICON = "icon"
        private const val ARG_TITLE = "title"
        private const val ARG_DESC = "desc"

        fun newInstance(@DrawableRes icon: Int, title: String, desc: String) =
            OnboardingPageFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ICON, icon)
                    putString(ARG_TITLE, title)
                    putString(ARG_DESC, desc)
                }
            }
    }
}
