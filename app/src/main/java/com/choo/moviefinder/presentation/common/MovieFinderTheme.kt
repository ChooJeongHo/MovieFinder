package com.choo.moviefinder.presentation.common

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// XML 쪽 DynamicColors.applyIfAvailable()와 동일한 Material You 팔레트를 Compose 화면(검색 결과,
// 온보딩)에도 적용한다. API 31 미만에서는 다이나믹 팔레트가 없어 기본 M3 팔레트로 안전하게 폴백한다.
// NewApi 억제 근거: dynamic*ColorScheme은 API 31이 필요하지만, resolveColorScheme()이 sdkInt >= S일 때만 이 람다를
// 호출한다. SDK 분기를 순수 함수로 분리하면서 Lint가 그 보호(람다 호출 조건)를 추적하지 못해 오탐이 되었다.
@SuppressLint("NewApi")
@Composable
fun MovieFinderTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val colorScheme = resolveColorScheme(
        sdkInt = Build.VERSION.SDK_INT,
        darkTheme = darkTheme,
        dynamicDark = { dynamicDarkColorScheme(context) },
        dynamicLight = { dynamicLightColorScheme(context) },
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}

// dynamicDark/dynamicLight를 람다로 주입받아, Context의 시스템 리소스를 실제로 건드리지 않고도
// 분기 로직만 순수 JVM 유닛 테스트로 검증할 수 있게 분리했다.
internal fun resolveColorScheme(
    sdkInt: Int,
    darkTheme: Boolean,
    dynamicDark: () -> ColorScheme,
    dynamicLight: () -> ColorScheme,
): ColorScheme = when {
    sdkInt >= Build.VERSION_CODES.S && darkTheme -> dynamicDark()
    sdkInt >= Build.VERSION_CODES.S -> dynamicLight()
    darkTheme -> darkColorScheme()
    else -> lightColorScheme()
}
