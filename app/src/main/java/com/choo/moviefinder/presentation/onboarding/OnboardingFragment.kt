package com.choo.moviefinder.presentation.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.choo.moviefinder.R
import com.choo.moviefinder.domain.usecase.CompleteOnboardingUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

// XML에서 제거된 것들:
//   - FragmentOnboardingBinding (ViewBinding)
//   - OnboardingPagerAdapter 내부 클래스 (FragmentStateAdapter)
//   - buildPages() / updateDots() / updateNextButton() / completeOnboarding() 메서드 4개
//   - onViewCreated() / onDestroyView() 생명주기 콜백 2개
//   - ViewPager2.OnPageChangeCallback 등록 코드
// → 모두 OnboardingScreen.kt 컴포저블로 이동

@AndroidEntryPoint
class OnboardingFragment : Fragment() {

    @Inject
    lateinit var completeOnboardingUseCase: CompleteOnboardingUseCase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val scope = rememberCoroutineScope()
            MaterialTheme {
                OnboardingScreen(
                    onComplete = {
                        scope.launch {
                            completeOnboardingUseCase()
                            findNavController().navigate(R.id.action_onboarding_to_home)
                        }
                    },
                )
            }
        }
    }
}
