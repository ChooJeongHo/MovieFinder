package com.choo.moviefinder.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupCold() {
        benchmarkRule.measureRepeated(
            packageName = "com.choo.moviefinder",
            metrics = listOf(StartupTimingMetric()),
            compilationMode = CompilationMode.DEFAULT,
            startupMode = StartupMode.COLD,
            iterations = 5
        ) {
            pressHome()
            startActivityAndWait()
        }
    }

    @Test
    fun startupWarm() {
        benchmarkRule.measureRepeated(
            packageName = "com.choo.moviefinder",
            metrics = listOf(StartupTimingMetric()),
            compilationMode = CompilationMode.DEFAULT,
            startupMode = StartupMode.WARM,
            iterations = 5
        ) {
            pressHome()
            startActivityAndWait()
        }
    }

    @Test
    fun scrollHomeScreen() {
        benchmarkRule.measureRepeated(
            packageName = "com.choo.moviefinder",
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.DEFAULT,
            startupMode = StartupMode.WARM,
            iterations = 5
        ) {
            startActivityAndWait()
            val list = device.findObject(By.res(packageName, "rv_movies"))
            list?.let {
                it.setGestureMargin(device.displayWidth / 5)
                it.fling(Direction.DOWN)
                device.waitForIdle()
                it.fling(Direction.UP)
            }
        }
    }
}
