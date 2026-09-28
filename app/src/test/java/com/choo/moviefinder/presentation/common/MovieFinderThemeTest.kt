package com.choo.moviefinder.presentation.common

import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MovieFinderThemeTest {

    @Test
    fun `API 31 이상 다크 모드에서는 dynamicDark 콜백 결과를 사용한다`() {
        val dynamicDarkScheme = darkColorScheme()

        val result = resolveColorScheme(
            sdkInt = Build.VERSION_CODES.S,
            darkTheme = true,
            dynamicDark = { dynamicDarkScheme },
            dynamicLight = { error("dynamicLight는 호출되면 안 된다") },
        )

        assertSame(dynamicDarkScheme, result)
    }

    @Test
    fun `API 31 이상 라이트 모드에서는 dynamicLight 콜백 결과를 사용한다`() {
        val dynamicLightScheme = lightColorScheme()

        val result = resolveColorScheme(
            sdkInt = Build.VERSION_CODES.S,
            darkTheme = false,
            dynamicDark = { error("dynamicDark는 호출되면 안 된다") },
            dynamicLight = { dynamicLightScheme },
        )

        assertSame(dynamicLightScheme, result)
    }

    @Test
    fun `API 31 미만 다크 모드에서는 정적 darkColorScheme으로 폴백한다`() {
        val result = resolveColorScheme(
            sdkInt = Build.VERSION_CODES.R,
            darkTheme = true,
            dynamicDark = { error("API 31 미만에서는 dynamic 콜백이 호출되면 안 된다") },
            dynamicLight = { error("API 31 미만에서는 dynamic 콜백이 호출되면 안 된다") },
        )

        assertEquals(darkColorScheme().primary, result.primary)
        assertEquals(darkColorScheme().background, result.background)
    }

    @Test
    fun `API 31 미만 라이트 모드에서는 정적 lightColorScheme으로 폴백한다`() {
        val result = resolveColorScheme(
            sdkInt = Build.VERSION_CODES.R,
            darkTheme = false,
            dynamicDark = { error("API 31 미만에서는 dynamic 콜백이 호출되면 안 된다") },
            dynamicLight = { error("API 31 미만에서는 dynamic 콜백이 호출되면 안 된다") },
        )

        assertEquals(lightColorScheme().primary, result.primary)
        assertEquals(lightColorScheme().background, result.background)
    }
}
