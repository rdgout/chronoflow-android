package net.chronoflow.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import net.chronoflow.core.testing.tiers.Perf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Perf
@RunWith(Parameterized::class)
class LaunchComparisonBenchmark(
    private val target: Target,
) {
    enum class Target(
        val packageName: String,
    ) {
        Native("net.chronoflow.app"),
        Rn("net.chronoflow.app"),
    }

    @get:Rule val rule = MacrobenchmarkRule()

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun coldSignedOut() =
        rule.measureRepeated(
            packageName = target.packageName,
            metrics = listOf(StartupTimingMetric(), FrameTimingMetric(), MemoryUsageMetric(MemoryUsageMetric.Mode.Max)),
            iterations = 10,
            startupMode = StartupMode.COLD,
            compilationMode = CompilationMode.Partial(BaselineProfileMode.UseIfAvailable),
            setupBlock = {
                device.clearApp(target.packageName)
                pressHome()
            },
        ) {
            startActivityAndWait()
            // Both apps get the same capture window, through the native Welcome intro's 3200 ms ending.
            device.wait(Until.hasObject(By.res("cf.never")), 3500)
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun targets(): List<Target> {
            val selected = InstrumentationRegistry.getArguments().getString("cf.compareTarget")
            return listOf(
                requireNotNull(Target.entries.singleOrNull { it.name == selected }) {
                    "cf.compareTarget must select Native or Rn for the installed APK"
                },
            )
        }
    }
}
