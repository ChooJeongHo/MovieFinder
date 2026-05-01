package com.choo.moviefinder.presentation.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.choo.moviefinder.R
import kotlinx.coroutines.launch

private data class OnboardingPageData(
    @param:DrawableRes val icon: Int,
    val title: String,
    val description: String,
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = listOf(
        OnboardingPageData(
            R.drawable.ic_search,
            stringResource(R.string.onboarding_page1_title),
            stringResource(R.string.onboarding_page1_desc),
        ),
        OnboardingPageData(
            R.drawable.ic_favorite,
            stringResource(R.string.onboarding_page2_title),
            stringResource(R.string.onboarding_page2_desc),
        ),
        OnboardingPageData(
            R.drawable.ic_stats,
            stringResource(R.string.onboarding_page3_title),
            stringResource(R.string.onboarding_page3_desc),
        ),
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Column(modifier = modifier.fillMaxSize()) {
        // XML: btn_skip (TextView, app:layout_constraintEnd_toEndOf="parent")
        // Compose: Box + Alignment.CenterEnd — ConstraintLayout 없이 동일 정렬
        Box(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = onComplete,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(8.dp),
            ) {
                Text(stringResource(R.string.onboarding_skip))
            }
        }

        // XML: ViewPager2 (FragmentStateAdapter → OnboardingPageFragment 생성)
        // Compose: HorizontalPager — Fragment 없이 람다로 직접 콘텐츠 선언
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            OnboardingPage(
                icon = pages[page].icon,
                title = pages[page].title,
                description = pages[page].description,
            )
        }

        // XML: dot_0/dot_1/dot_2 — View 3개 하드코딩 + updateDots()로 배경 수동 교체
        // Compose: repeat(pages.size) — 페이지 수가 달라져도 코드 변경 없음
        //          animateColorAsState — 페이지 전환 시 색상 자동 애니메이션
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(pages.size) { i ->
                val color by animateColorAsState(
                    targetValue = if (i == pagerState.currentPage) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    },
                    label = "dot_color",
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }

        // XML: btn_next (MaterialButton) + updateNextButton() — 수동 setText()
        // Compose: isLastPage State가 바뀌면 Text가 자동 리컴포지션
        //          pagerState.animateScrollToPage() — ViewPager2.currentItem 대신 suspend 호출
        Button(
            onClick = {
                if (isLastPage) {
                    onComplete()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
        ) {
            Text(stringResource(if (isLastPage) R.string.onboarding_start else R.string.onboarding_next))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    MaterialTheme {
        OnboardingScreen(onComplete = {})
    }
}
