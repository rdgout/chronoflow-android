package net.chronoflow.feature.launch

import android.graphics.Rect
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.launch.SessionPresence
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.launch.model.LaunchSize
import net.chronoflow.feature.launch.model.ManualLaunchClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LaunchTraceTest {
    @Test fun readyAndExitPairOnceAtTheirLaunchClockDeadlines() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            val events = mutableListOf<String>()
            val trace =
                LaunchTraceSession({ name, cookie -> events += "begin:$name:$cookie:${clock.now}" }, { name, cookie ->
                    events +=
                        "end:$name:$cookie:${clock.now}"
                })
            val source =
                object : LaunchReadiness {
                    override suspend fun session() = SessionPresence.SignedIn("a")

                    override suspend fun gate() = Gate.App

                    override suspend fun hasStoredData() = true

                    override suspend fun allowFirstLoad() = Unit
                }
            val controller =
                LaunchController(moon = {
                    error("no decode")
                }, nanoTime = {
                    0
                }, tracing = trace, scope = backgroundScope, clockFactory = { clock }, readinessFactory = { source })
            trace.beginLaunch()
            controller.captureReduceMotion(true, false)
            controller.onActivityCreated(null, false)
            controller.onOverlayGeometry(
                net.chronoflow.feature.launch.model
                    .LaunchSize(390.0, 844.0),
                net.chronoflow.feature.launch.model.MoonTarget
                    .Insets(0.0, 0.0, 0.0, 0.0),
                28.0,
            )
            controller.onFirstDraw(Rect())
            runCurrent()
            clock.advance(1499.milliseconds)
            runCurrent()
            assertTrue(events.none { it.startsWith("begin:exit") })
            clock.advance(1500.milliseconds)
            runCurrent()
            assertEquals("begin:exit:4:1.5s", events.last())
            clock.advance(1900.milliseconds)
            controller.onExitFrame(1_900_000_000)
            controller.onExitFrame(2_000_000_000)
            controller.onActivityDestroyed()
            trace.beginLaunch()
            trace.beginReady()
            assertEquals(
                listOf(
                    "begin:launch:1:0s",
                    "begin:first-commit:2:0s",
                    "begin:ready:3:0s",
                    "end:first-commit:2:0s",
                    "end:ready:3:0s",
                    "begin:exit:4:1.5s",
                    "end:exit:4:1.9s",
                    "end:launch:1:1.9s",
                ),
                events,
            )
        }

    @Test fun diagnosticModeTracesTheExitWithoutBeginningReady() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            val events = mutableListOf<String>()
            val trace = LaunchTraceSession({ name, _ -> events += "begin:$name" }, { name, _ -> events += "end:$name" })
            val controller =
                LaunchController(moon = {
                    error("no decode")
                }, nanoTime = { 0 }, tracing = trace, scope = backgroundScope, clockFactory = { clock })
            trace.beginLaunch()
            controller.captureReduceMotion(true, false)
            controller.onActivityCreated(null, false, LaunchOverrides(mode = LaunchMode.Ready))
            controller.setOpening(LaunchSize(390.0, 844.0))
            controller.onOverlayGeometry(
                net.chronoflow.feature.launch.model
                    .LaunchSize(390.0, 844.0),
                net.chronoflow.feature.launch.model.MoonTarget
                    .Insets(0.0, 0.0, 0.0, 0.0),
                28.0,
            )
            controller.onFirstDraw(Rect())
            runCurrent()
            assertNull(controller.coordinator)
            assertNull(controller.route)
            clock.advance(1500.milliseconds)
            runCurrent()
            assertTrue("begin:exit" in events)
            controller.onExitFrame(1_900_000_000)
            assertTrue(events.none { it.contains("ready") })
            assertEquals(listOf("end:exit", "end:launch"), events.takeLast(2))
        }
}
