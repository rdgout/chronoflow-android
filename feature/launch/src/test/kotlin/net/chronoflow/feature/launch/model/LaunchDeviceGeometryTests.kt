package net.chronoflow.feature.launch.model

import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.feature.launch.generated.LaunchWordmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.max
import kotlin.math.min

@RunWith(Parameterized::class)
class LaunchDeviceGeometryTests(
    private val profile: DeviceProfile,
) {
    @Test fun sampledInkIsContainedAndCompositionKeepsBoardClearances() {
        val size = LaunchSize(profile.widthDp.toDouble(), profile.heightDp.toDouble())
        val own = profile.insets
        for (insets in listOf(
            MoonTarget.Insets(own.top.toDouble(), own.left.toDouble(), own.bottom.toDouble(), own.right.toDouble()),
            MoonTarget.Insets(7.0, 19.0, 11.0, 31.0),
        )) {
            val layout = LaunchLayout(size, insets)
            val sampler = LaunchFrameSampler(LaunchTimeline.opening(size, false))
            val frame = LaunchFrame()

            fun rect(
                x: Double,
                y: Double,
                width: Double,
                height: Double,
            ): LaunchRect =
                LaunchRect(
                    size.width / 2 + (x - size.width / 2 - width / 2) * frame.containerScale,
                    size.height * 0.4 + (y - size.height * 0.4 - height / 2) * frame.containerScale,
                    width * frame.containerScale,
                    height * frame.containerScale,
                )
            for (ms in listOf(0, 300, 700, 1000, 1100, 1400, 1500)) {
                sampler.sampleInto(ms / 1000.0, frame)
                val moon = rect(frame.moonX, frame.moonY, 104 * frame.moonScale, 104 * frame.moonScale)
                assertTrue("${profile.name}: moon at $ms", layout.safeBounds.contains(moon, 0.5))
                if (frame.wordmarkOpacity > 0) {
                    val mark =
                        rect(
                            layout.wordmarkCentre.x,
                            layout.wordmarkCentre.y + frame.wordmarkTranslationY,
                            LaunchWordmark.WIDTH_DP.toDouble(),
                            40.0,
                        )
                    assertTrue("${profile.name}: wordmark at $ms", layout.safeBounds.contains(mark))
                }
                if (frame.sunOpacity > 0) {
                    val sun = rect(layout.sunCentre.x, layout.sunCentre.y + frame.sunTranslationY, 180.0, 180.0)
                    val bounds = LaunchRect(insets.left, 0.0, size.width - insets.left - insets.right, size.height)
                    val visible = max(0.0, min(sun.maxY, bounds.maxY) - max(sun.minY, bounds.minY))
                    assertTrue(
                        "${profile.name}: sun at $ms: $sun visible=$visible",
                        sun.minX >= bounds.minX - 0.5 &&
                            sun.maxX <= bounds.maxX + 0.5 &&
                            sun.minY >= -0.5 &&
                            sun.minY < bounds.maxY &&
                            visible >= sun.height / 3,
                    )
                }
            }
            val apex = FlightPath(size).pointAtFraction(0.5).y
            assertEquals(5.5, layout.status.minY - apex, 0.001)
            assertTrue(layout.status.maxY < layout.sunCentre.y - 90)
            assertTrue(layout.wordmarkCentre.y + 20 < apex)
            val reduced = LaunchFrameSampler(LaunchTimeline.opening(size, true))
            reduced.sampleInto(0.0, frame)
            assertTrue(layout.safeBounds.contains(rect(frame.moonX, frame.moonY, 104.0, 104.0), 0.5))
        }
    }

    @Test fun splitScreenKeepsEverySampleFinite() {
        val sampler = LaunchFrameSampler(LaunchTimeline.opening(LaunchSize(360.0, 300.0), false))
        val frame = LaunchFrame()
        for (ms in listOf(0, 300, 700, 1000, 1100, 1400, 1500)) {
            sampler.sampleInto(ms / 1000.0, frame)
            assertTrue(
                listOf(
                    frame.moonX,
                    frame.moonY,
                    frame.planeX,
                    frame.planeY,
                    frame.sunTranslationY,
                    frame.moonScale,
                ).all {
                    it.isFinite()
                },
            )
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<Array<Any>> =
            (DeviceProfiles.all + DeviceProfiles.small.copy(name = "minimum280", widthDp = 280, heightDp = 653)).map {
                arrayOf<Any>(it)
            }
    }
}
