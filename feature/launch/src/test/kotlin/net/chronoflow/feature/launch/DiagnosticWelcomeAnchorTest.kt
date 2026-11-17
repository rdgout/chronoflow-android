package net.chronoflow.feature.launch

import android.graphics.Rect
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
class DiagnosticWelcomeAnchorTest {
    @Test fun diagnosticAnchorRebuildsBeforeAndAfterTheFlightStarts() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            val controller =
                LaunchController(moon = { error("no drawing") }, scope = backgroundScope, clockFactory = { clock })
            controller.captureReduceMotion(true, false)
            controller.onActivityCreated(
                null,
                false,
                LaunchOverrides(mode = LaunchMode.Welcome, freezeAt = 1950.milliseconds),
            )
            controller.onOverlayGeometry(LaunchSize(390.0, 844.0), MoonTarget.Insets(0.0, 0.0, 0.0, 0.0), 28.0)
            controller.setOpening(LaunchSize(390.0, 844.0))
            controller.onFirstDraw(Rect())
            runCurrent()
            assertEquals(LaunchRoute.Welcome, controller.route)
            assertNull(controller.coordinator)
            val intro = requireNotNull(controller.launchIntro)
            assertEquals(2300.milliseconds, intro.start)
            for (ms in listOf(1000, 2000)) {
                clock.advance(ms.milliseconds)
                val target = LaunchRect(ms / 10.0, 64.0, 28.0, 28.0)
                controller.moonAnchorStore.update(target)
                val frame = LaunchFrame()
                controller.handOff.sampler.sampleInto(2.3, frame)
                assertEquals(target.midX, frame.moonX, 1e-9)
                assertEquals(target.midY, frame.moonY, 1e-9)
                assertEquals(1950.milliseconds, controller.overrides.freezeAt)
                assertEquals(1950.milliseconds, intro.frozenTime)
                assertSame(intro, controller.launchIntro)
            }
        }
}
