package com.choo.moviefinder.presentation.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import com.choo.moviefinder.databinding.FragmentOnboardingPageBinding

class OnboardingPageFragment : Fragment() {

    private var _binding: FragmentOnboardingPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        binding.ivOnboardingIcon.setImageResource(args.getInt(ARG_ICON))
        binding.tvOnboardingTitle.text = args.getString(ARG_TITLE)
        binding.tvOnboardingDescription.text = args.getString(ARG_DESC)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ICON = "icon"
        private const val ARG_TITLE = "title"
        private const val ARG_DESC = "desc"

        fun newInstance(@DrawableRes icon: Int, title: String, desc: String): OnboardingPageFragment {
            return OnboardingPageFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ICON, icon)
                    putString(ARG_TITLE, title)
                    putString(ARG_DESC, desc)
                }
            }
        }
    }
}
