package net.chronoflow.feature.launch

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.model.LaunchFrame
import org.junit.Assert.assertArrayEquals
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
class LaunchReduceMotionEndingTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun reduceMotionFadesWithoutScalingAtReadiness() {
        val host = LaunchEndingTestHost(compose, readyMs = 50, reduced = true)
        host.advanceTo(200)
        val start = requireNotNull(host.controller.coordinator).state.exitStart!!.inWholeNanoseconds / 1e9
        assertTrue(start in 0.05..0.058)
        assertEquals(
            1 - 0.6846431874,
            host.controller.handOff.sampler
                .containerOpacity(start + 0.15),
            0.01,
        )
        assertEquals(
            1.0,
            host.controller.handOff.sampler
                .containerScale(0.2),
            0.0,
        )
        host.advanceTo(((start + 0.3) * 1000).toInt() + 8)
        assertTrue(host.controller.handOffComplete)
        assertTrue(
            requireNotNull(host.controller.handOffEndElapsedNanos) in
                ((start + 0.3) * 1e9).toLong()..((start + 0.308) * 1e9).toLong(),
        )
    }

    @Test fun reduceMotionSlowStatusHasConstantDots() {
        val host = LaunchEndingTestHost(compose, readyMs = 3500, reduced = true)
        host.advanceTo(2300)
        val frame = LaunchFrame()
        host.controller.handOff.sampler
            .sampleInto(2.3, frame)
        assertEquals(1.0, frame.statusOpacity, 0.0)
        assertArrayEquals(doubleArrayOf(1.0, 1.0, 1.0), frame.statusDotOpacity, 0.0)
    }
}
