package net.chronoflow.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.core.testing.tiers.Perf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
            metrics = listOf(StartupTimingMetric()),
            iterations = 10,
            // UseIfAvailable until 1e commits the profiles; Partial() alone defaults to Require.
            compilationMode = CompilationMode.Partial(BaselineProfileMode.UseIfAvailable),
            startupMode = StartupMode.COLD,
            setupBlock = { pressHome() },
        ) {
            startActivityAndWait()
        }
}
