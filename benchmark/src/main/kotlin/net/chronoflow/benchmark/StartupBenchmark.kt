package net.chronoflow.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import net.chronoflow.core.testing.tiers.Perf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMetricApi::class)
@Perf
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule val rule = MacrobenchmarkRule()

    private val packageName: String =
        InstrumentationRegistry.getArguments().getString("targetAppId") ?: "net.chronoflow.app.native"

    @Test
    fun startupCold() =
        rule.measureRepeated(
            packageName = packageName,
            metrics =
                listOf(
                    StartupTimingMetric(),
                    TraceSectionMetric("first-commit", TraceSectionMetric.Mode.First),
                    TraceSectionMetric("ready", TraceSectionMetric.Mode.First),
                    TraceSectionMetric("exit", TraceSectionMetric.Mode.First),
                ),
            iterations = 10,
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            startupMode = StartupMode.COLD,
            setupBlock = { pressHome() },
        ) {
            startActivityAndWait()
            check(device.wait(Until.gone(By.res("launch.opening")), 5000))
            check(device.wait(Until.hasObject(By.res("route.welcome")), 2000))
            device.waitForIdle(1000)
        }

    @Test fun startupWarm() =
        rule.measureRepeated(
            packageName = packageName,
            metrics = listOf(StartupTimingMetric()),
            iterations = 10,
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            startupMode = StartupMode.WARM,
            setupBlock = { pressHome() },
        ) {
            startActivityAndWait()
            // Macrobenchmark invokes this block cold with iteration=null before the first WARM iteration.
            if (iteration == null) check(device.wait(Until.gone(By.res("launch.opening")), 5000))
            check(!device.hasObject(By.res("launch.opening")))
            check(device.wait(Until.hasObject(By.res("route.welcome")), 2000))
        }
}
