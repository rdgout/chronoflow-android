package net.chronoflow.benchmark

import android.graphics.Rect
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.SearchCondition
import androidx.test.uiautomator.Searchable
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import net.chronoflow.core.testing.BoundedProfileInput
import net.chronoflow.core.testing.tiers.Perf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Perf
@RunWith(AndroidJUnit4::class)
class TodayTitleBenchmark {
    @get:Rule val rule = MacrobenchmarkRule()

    private val packageName =
        InstrumentationRegistry.getArguments().getString("targetAppId") ?: "net.chronoflow.app"

    @Test fun todayTitleCollapse() =
        rule.measureRepeated(
            packageName = packageName,
            metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
            iterations = 3,
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            startupMode = StartupMode.COLD,
            setupBlock = {
                assertAnimatorsOn()
                pressHome()
            },
        ) {
            BoundedProfileInput(journeyTimeoutMillis = 60_000).use { input ->
                input.step("signed-in Today cold launch") {
                    startActivityAndWait(signedInStartupIntent(packageName))
                }
                input.step("launch overlay closed") {
                    check(device.wait(Until.gone(By.res("launch.opening")), 5_000)) { "launch overlay never closed" }
                }
                input.step("Today fixture ready") { device.requireTag("today.trip") }
                val expanded = input.step("expanded Today bar") { device.requireTag("today.appbar").visibleBounds }
                repeat(3) { round ->
                    input.step("Today up fling ${round + 1}/3") { device.flingToday(up = true) }
                    input.step("Today bar collapsed ${round + 1}/3") {
                        device.awaitTodayBar("collapse") { it.height() < expanded.height() }
                    }
                    var expandedAgain = false
                    for (attempt in 1..4) {
                        input.step("Today down fling ${round + 1}/3, attempt $attempt/4") {
                            device.flingToday(up = false)
                        }
                        expandedAgain =
                            input.step("Today bar expanded ${round + 1}/3, attempt $attempt/4") {
                                device.waitForTodayBar(750) { it.height() >= expanded.height() }
                            }
                        if (expandedAgain) break
                    }
                    check(expandedAgain) { "Today app bar did not expand after its fling" }
                }
            }
        }
}

private fun UiDevice.flingToday(up: Boolean) {
    val list = requireTag("today.screen").visibleBounds
    val bar = requireTag("today.appbar").visibleBounds
    val top = maxOf(list.top, bar.bottom)
    val height = list.bottom - top
    check(height > 0) { "Today list has no gesture area below the app bar" }
    val high = top + height / 5
    val low = top + height * 4 / 5
    check(swipe(list.centerX(), if (up) low else high, list.centerX(), if (up) high else low, 5)) {
        "Today fling injection failed"
    }
}

private fun UiDevice.awaitTodayBar(
    phase: String,
    matches: (Rect) -> Boolean,
) {
    check(waitForTodayBar(5_000, matches)) { "Today app bar did not $phase after its fling" }
}

private fun UiDevice.waitForTodayBar(
    timeoutMillis: Long,
    matches: (Rect) -> Boolean,
): Boolean =
    wait(
        object : SearchCondition<Boolean>() {
            override fun apply(searchable: Searchable): Boolean =
                searchable.findObject(By.res("today.appbar"))?.visibleBounds?.let(matches) == true
        },
        timeoutMillis,
    )
