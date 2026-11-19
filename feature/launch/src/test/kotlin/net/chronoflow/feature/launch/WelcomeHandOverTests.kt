package net.chronoflow.feature.launch

import android.graphics.Rect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.launch.model.LaunchFrame
import net.chronoflow.feature.launch.model.LaunchRect
import net.chronoflow.feature.launch.model.LaunchRoute
import net.chronoflow.feature.launch.model.LaunchSize
import net.chronoflow.feature.launch.model.ManualLaunchClock
import net.chronoflow.feature.launch.model.MoonTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
class WelcomeHandOverTests {
    private val size = LaunchSize(390.0, 844.0)
    private val safe = MoonTarget.Insets(47.0, 0.0, 34.0, 0.0)

    private fun TestScope.controller(
        clock: ManualLaunchClock,
        rm: Boolean = false,
        row: Double = 28.0,
        tracing: LaunchTracing = LaunchTrace,
        work: CoroutineScope = backgroundScope,
    ): LaunchController =
        LaunchController(
            moon = { error("no drawing") },
            nanoTime = { 0L },
            scope = work,
            clockFactory = { clock },
            tracing = tracing,
        ).apply {
            captureReduceMotion(true, rm)
            onActivityCreated(null, false)
            onOverlayGeometry(size, safe, row)
            setOpening(size)
            onFirstDraw(Rect())
        }

    @Test fun fallbackComesFromTheOverlayGeometry() =
        runTest(timeout = 60.seconds) {
            for (row in listOf(28.0, 36.4)) {
                val c = controller(ManualLaunchClock(), row = row)
                runCurrent()
                assertEquals(
                    LaunchRect(24.0, 64.0 + (row - 28) / 2, 28.0, 28.0),
                    requireNotNull(c.coordinator).state.fallbackTarget,
                )
            }
        }

    @Test fun introIsCreatedOnceAtExitStartPlus700BeforeTheRoute() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock().apply { advance(10.milliseconds) }
            val c = controller(clock)
            runCurrent()
            assertEquals(LaunchRoute.Welcome, c.route)
            val intro = requireNotNull(c.launchIntro)
            assertEquals(2300.milliseconds, intro.start)
            c.moonAnchorStore.update(LaunchRect(24.0, 64.0, 28.0, 28.0))
            assertSame(intro, c.launchIntro)
        }

    @Test fun noIntroWithReduceMotionOrWithoutTheOverlay() =
        runTest(timeout = 60.seconds) {
            val c = controller(ManualLaunchClock(), rm = true)
            runCurrent()
            assertEquals(LaunchRoute.Welcome, c.route)
            assertNull(c.launchIntro)
            val headlessClock = ManualLaunchClock()
            val headless =
                LaunchController(
                    moon = { error("no drawing") },
                    scope = backgroundScope,
                    clockFactory = { headlessClock },
                )
            headless.onActivityCreated(null, true)
            runCurrent()
            assertEquals(LaunchRoute.Welcome, headless.route)
            assertNull(headless.launchIntro)
        }

    @Test fun introTraceSpansStartToStartPlus900() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            val events = mutableListOf<String>()
            val trace =
                LaunchTraceSession(
                    { name, _ -> if (name == "intro") events += "begin:${clock.now}" },
                    { name, _ -> if (name == "intro") events += "end:${clock.now}" },
                )
            controller(clock, tracing = trace)
            runCurrent()
            clock.advance(2300.milliseconds)
            runCurrent()
            assertEquals(listOf("begin:2.3s"), events)
            clock.advance(3200.milliseconds)
            runCurrent()
            assertEquals(listOf("begin:2.3s", "end:3.2s"), events)
        }

    @Test fun cancellingTheIntroTraceEndsItOnce() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            val events = mutableListOf<String>()
            val trace =
                LaunchTraceSession(
                    { name, _ -> if (name == "intro") events += "begin" },
                    { name, _ -> if (name == "intro") events += "end" },
                )
            val work = CoroutineScope(backgroundScope.coroutineContext + Job(backgroundScope.coroutineContext[Job]))
            controller(clock, tracing = trace, work = work)
            runCurrent()
            clock.advance(2500.milliseconds)
            runCurrent()
            work.cancel()
            runCurrent()
            assertEquals(listOf("begin", "end"), events)
        }

    @Test fun sizeChangeBeforeFlightUsesTheNewFallback() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            val c = controller(clock)
            runCurrent()
            clock.advance(1200.milliseconds)
            c.onOverlayGeometry(LaunchSize(570.0, 730.0), MoonTarget.Insets(31.0, 37.0, 27.0, 59.0), 40.0)
            runCurrent()
            val fallback =
                MoonTarget.fallback(
                    LaunchSize(570.0, 730.0),
                    MoonTarget.Insets(31.0, 37.0, 27.0, 59.0),
                    40.0,
                )
            assertEquals(fallback, requireNotNull(c.coordinator).state.fallbackTarget)
            clock.advance(1550.milliseconds)
            runCurrent()
            val f = LaunchFrame()
            c.handOff.sampler.sampleInto(2.3, f)
            assertEquals(fallback.midX, f.moonX, 1e-9)
            assertEquals(fallback.midY, f.moonY, 1e-9)
        }

    @Test fun headlessCoordinatorUsesANonZeroFallback() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            val c =
                LaunchController(
                    moon = { error("no drawing") },
                    scope = backgroundScope,
                    clockFactory = { clock },
                )
            val initial = c.handOff
            c.onActivityCreated(null, true)
            runCurrent()
            assertEquals(LaunchRoute.Welcome, c.route)
            assertEquals(28.0, requireNotNull(c.coordinator).state.fallbackTarget.width, 0.0)
            assertSame(initial, c.handOff)
            assertNull(c.launchIntro)
            assertNotNull(c.moonAnchorStore)
        }
}
