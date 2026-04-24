package com.choo.moviefinder.presentation.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.choo.moviefinder.R
import com.choo.moviefinder.data.local.UserSettings
import com.choo.moviefinder.data.local.UserSettingsSerializer
import com.choo.moviefinder.databinding.FragmentOnboardingBinding
import kotlinx.coroutines.launch
import androidx.datastore.core.DataStoreFactory
import java.io.File

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val dots get() = listOf(binding.dot0, binding.dot1, binding.dot2)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pages = buildPages()
        binding.viewPager.adapter = OnboardingPagerAdapter(requireActivity(), pages)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                updateNextButton(position, pages.size)
            }
        })

        binding.btnSkip.setOnClickListener { completeOnboarding() }

        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < pages.size - 1) {
                binding.viewPager.currentItem = current + 1
            } else {
                completeOnboarding()
            }
        }

        updateDots(0)
        updateNextButton(0, pages.size)
    }

    private fun buildPages(): List<Triple<Int, String, String>> {
        val res = requireContext().resources
        return listOf(
            Triple(
                R.drawable.ic_search,
                res.getString(R.string.onboarding_page1_title),
                res.getString(R.string.onboarding_page1_desc)
            ),
            Triple(
                R.drawable.ic_favorite,
                res.getString(R.string.onboarding_page2_title),
                res.getString(R.string.onboarding_page2_desc)
            ),
            Triple(
                R.drawable.ic_stats,
                res.getString(R.string.onboarding_page3_title),
                res.getString(R.string.onboarding_page3_desc)
            )
        )
    }

    private fun updateDots(position: Int) {
        dots.forEachIndexed { index, dot ->
            val bg = if (index == position) R.drawable.onboarding_dot_active else R.drawable.onboarding_dot_inactive
            dot.setBackgroundResource(bg)
        }
    }

    private fun updateNextButton(position: Int, pageCount: Int) {
        binding.btnNext.text = getString(
            if (position == pageCount - 1) R.string.onboarding_start else R.string.onboarding_next
        )
    }

    private fun completeOnboarding() {
        val context = requireContext().applicationContext
        val dataStore = DataStoreFactory.create(
            serializer = UserSettingsSerializer,
            produceFile = { File(context.filesDir, "datastore/user_settings.json") }
        )
        lifecycleScope.launch {
            dataStore.updateData { current: UserSettings ->
                current.copy(onboardingCompleted = true)
            }
            findNavController().navigate(R.id.action_onboarding_to_home)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class OnboardingPagerAdapter(
        activity: FragmentActivity,
        private val pages: List<Triple<Int, String, String>>
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = pages.size

        override fun createFragment(position: Int): Fragment {
            val (icon, title, desc) = pages[position]
            return OnboardingPageFragment.newInstance(icon, title, desc)
        }
    }
}
