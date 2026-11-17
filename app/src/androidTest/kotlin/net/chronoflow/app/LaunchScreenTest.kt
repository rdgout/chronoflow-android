package net.chronoflow.app

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.feature.launch.LaunchController
import org.junit.Assert.assertTrue

abstract class LaunchScreenTest : ScreenTest() {
    protected val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    protected val graph get() = (instrumentation.targetContext.applicationContext as ChronoFlowApp).graph

    protected fun resetLaunch() = instrumentation.runOnMainSync { graph.resetLaunchForTest() }

    protected fun <T : Any> read(block: (LaunchController) -> T): T {
        var result: T? = null
        instrumentation.runOnMainSync { result = block(graph.launch) }
        return requireNotNull(result)
    }

    protected fun frameMillis(scenario: ActivityScenario<MainActivity>): Double {
        var frame = 0.0
        scenario.onActivity { frame = 1000.0 / checkNotNull(it.window.decorView.display).refreshRate }
        return frame
    }

    protected fun assertEnd(
        label: String,
        expectedMs: Double,
        frameMs: Double,
    ) {
        val end = read { requireNotNull(it.handOffEndElapsedNanos) / 1e6 }
        Log.i("CfLaunchProof", "$label handOffEndMs=$end frameMs=$frameMs")
        assertTrue(
            "$label ended at $end ms; expected $expectedMs..${expectedMs + frameMs + 2}",
            end >= expectedMs && end <= expectedMs + frameMs + 2,
        )
    }

    protected fun seed(vararg extras: Pair<String, String>): Map<String, String> =
        mapOf("CF_UI_TEST_PROFILE" to "launch", "cf.reduceMotion" to "false") + extras
}
