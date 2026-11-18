package net.chronoflow.app

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.feature.launch.LaunchController
import net.chronoflow.feature.launch.model.LaunchState
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

    protected fun exitStartMillis(): Double =
        read { controller ->
            // Inspect private state in the test rather than adding a production accessor.
            val field = LaunchController::class.java.getDeclaredField("launchState").apply { isAccessible = true }
            val state = field.get(controller) as LaunchState
            requireNotNull(state.exitStart).inWholeNanoseconds / 1e6
        }

    protected fun assertEnd(
        label: String,
        expectedMs: Double,
        frameMs: Double,
    ) {
        val end = read { requireNotNull(it.handOffEndElapsedNanos) / 1e6 }
        assertInFrameWindow("$label handOffEnd", end, expectedMs, frameMs)
    }

    protected fun assertInFrameWindow(
        label: String,
        actualMs: Double,
        expectedMs: Double,
        frameMs: Double,
    ) {
        Log.i("CfLaunchProof", "$label actualMs=$actualMs expectedMs=$expectedMs frameMs=$frameMs")
        // The elapsed-time read happens after frame resumption, so allow half a frame of dispatch latency.
        val upperMs = expectedMs + frameMs + frameMs / 2
        assertTrue(
            "$label was $actualMs ms; expected $expectedMs..$upperMs",
            actualMs >= expectedMs && actualMs <= upperMs,
        )
    }

    protected fun seed(vararg extras: Pair<String, String>): Map<String, String> =
        mapOf("CF_UI_TEST_PROFILE" to "launch", "cf.reduceMotion" to "false") + extras
}
