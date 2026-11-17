package net.chronoflow.feature.launch.model

import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.milliseconds

@RunWith(Parameterized::class)
class WelcomeCompositionTest(
    private val profile: DeviceProfile,
) {
    @Test fun compositionAndFlightStayInsideEveryHeight() {
        val insets =
            profile.insets.let {
                MoonTarget.Insets(it.top.toDouble(), it.left.toDouble(), it.bottom.toDouble(), it.right.toDouble())
            }
        val frame = LaunchFrame()
        for (height in 568..1400) {
            val size = LaunchSize(profile.widthDp.toDouble(), height.toDouble())
            val g = WelcomeGeometry(size, insets)
            assertEquals(MoonTarget.contentBounds(size, insets), g.content)
            assertEquals(maxOf(insets.top + 16, 64.0), g.top, 0.0)
            assertEquals(maxOf(insets.bottom, 40.0), g.bottom, 0.0)
            assertEquals(insets.left + 24, g.leading, 0.0)
            assertEquals(insets.right + 24, g.trailing, 0.0)
            val target = MoonTarget.fallback(size, insets, 28.0)
            val window = LaunchRect(0.0, 0.0, size.width, size.height)
            assertTrue(g.content.contains(target))
            assertTrue(window.contains(target))
            val sampler =
                LaunchFrameSampler(LaunchTimeline.opening(size, false).withWelcomeExit(1600.milliseconds, size, target))
            for (seconds in listOf(1.6, 1.95, 2.3, 2.55)) {
                sampler.sampleInto(seconds, frame)
                assertTrue(frame.moonX in 0.0..size.width && frame.moonY in 0.0..size.height)
                assertTrue(
                    listOf(frame.moonX, frame.moonY, frame.moonScale, frame.containerOpacity).all { it.isFinite() },
                )
            }
            assertEquals(0.0, frame.containerOpacity, 1e-12)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun profiles(): List<Array<DeviceProfile>> =
            (
                DeviceProfiles.all +
                    DeviceProfiles.small.copy(
                        name = "minimum280",
                        widthDp = 280,
                        heightDp = 653,
                    )
            ).map { arrayOf(it) }
    }
}
