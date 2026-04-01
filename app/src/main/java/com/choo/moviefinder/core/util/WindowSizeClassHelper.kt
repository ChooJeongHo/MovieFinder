package com.choo.moviefinder.core.util

import android.app.Activity
import androidx.window.layout.WindowMetricsCalculator

enum class WindowWidthSizeClass { COMPACT, MEDIUM, EXPANDED }

/**
 * 현재 Activity 창의 너비를 기준으로 WindowWidthSizeClass를 계산한다.
 * - COMPACT  : 600dp 미만 (일반 폰 세로 모드) → 그리드 2열
 * - MEDIUM   : 600dp ~ 840dp 미만 (폴더블/태블릿 세로 모드) → 그리드 3열
 * - EXPANDED : 840dp 이상 (태블릿 가로 모드) → 그리드 4열
 */
fun Activity.computeWindowWidthSizeClass(): WindowWidthSizeClass {
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
    val widthDp = metrics.bounds.width() / resources.displayMetrics.density
    return when {
        widthDp < 600f -> WindowWidthSizeClass.COMPACT
        widthDp < 840f -> WindowWidthSizeClass.MEDIUM
        else -> WindowWidthSizeClass.EXPANDED
    }
}

/**
 * WindowWidthSizeClass에 따른 영화 그리드 열 수를 반환한다.
 */
fun WindowWidthSizeClass.toMovieGridSpanCount(): Int = when (this) {
    WindowWidthSizeClass.COMPACT -> 2
    WindowWidthSizeClass.MEDIUM -> 3
    WindowWidthSizeClass.EXPANDED -> 4
}
