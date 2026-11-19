package net.chronoflow.feature.launch

import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.model.ManualLaunchClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.milliseconds

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchOverlayRecompositionTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun wallClockAnimationComposesOnlyOnceAndStopsAtHandOff() {
        compose.mainClock.autoAdvance = false
        var compositions = 0
        var pendingFrames = 0
        val manual = ManualLaunchClock()
        val controller =
            launchController(compose, ms = null, observer = { compositions++ }, clockFactory = { manual }, nextFrame = {
                pendingFrames++
                try {
                    androidx.compose.runtime.withFrameNanos { it }
                } finally {
                    pendingFrames--
                }
            })
        compose.setContent { ProfiledLaunchOverlay(controller) }
        firstDraw(compose, controller)
        repeat(330) {
            compose.mainClock.advanceTimeBy(8, ignoreFrameDuration = true)
            manual.advance(
                (compose.mainClock.currentTime - controller.t0Nanos / 1_000_000).milliseconds,
            )
            compose.waitForIdle()
        }
        compose.onNodeWithTag("launch.opening").captureToImage()
        assertEquals(1, compositions)
        assertTrue(controller.openingDone)
        compose.runOnIdle { assertEquals(0, pendingFrames) }
    }
}
