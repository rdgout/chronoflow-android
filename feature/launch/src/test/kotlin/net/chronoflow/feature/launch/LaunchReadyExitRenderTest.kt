package net.chronoflow.feature.launch

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchReadyExitRenderTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun readyFadesAndLeavesOnTheFirstFrameAt1900() {
        val host = LaunchEndingTestHost(compose)
        host.advanceTo(1700)
        val sampler = host.controller.handOff.sampler
        assertEquals(1 - 0.6846431874, sampler.containerOpacity(1.7), 0.01)
        assertEquals(1 + 0.08 * 0.6846431874, sampler.containerScale(1.7), 0.001)
        host.advanceTo(1896)
        assertTrue(!host.controller.handOffComplete)
        host.advanceTo(1904)
        assertTrue(host.controller.handOffComplete)
        assertTrue(requireNotNull(host.controller.handOffEndElapsedNanos) in 1_900_000_000..1_908_000_000)
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("launch.opening").assertDoesNotExist()
        assertTrue(host.compositions <= 2)
    }
}
