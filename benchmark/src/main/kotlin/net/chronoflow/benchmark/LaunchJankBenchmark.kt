package net.chronoflow.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import net.chronoflow.core.testing.tiers.Perf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Perf
@RunWith(AndroidJUnit4::class)
class LaunchJankBenchmark {
    @get:Rule val rule = MacrobenchmarkRule()
    private val packageName =
        InstrumentationRegistry.getArguments().getString("targetAppId") ?: "net.chronoflow.app"

    @Test fun launchCold() =
        rule.measureRepeated(
            packageName = packageName,
            metrics = listOf(FrameTimingMetric()),
            iterations = 10,
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            startupMode = StartupMode.COLD,
            setupBlock = { pressHome() },
        ) {
            startActivityAndWait()
            check(device.wait(Until.gone(By.res("launch.opening")), 3500))
            check(device.wait(Until.hasObject(By.res("welcome.signIn")), 1000))
            // Sign in remains on Welcome, so this intentionally times out after the intro has ended.
            device.wait(Until.gone(By.res("welcome.signIn")), 900)
        }
}
