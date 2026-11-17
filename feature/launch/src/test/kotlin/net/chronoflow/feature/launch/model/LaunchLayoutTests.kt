package net.chronoflow.feature.launch.model

import net.chronoflow.core.testing.DeviceProfiles
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchLayoutTests {
    @Test fun everyD1cSizeKeepsTheBoardOffsetsAndStretchesTheBand() {
        val sizes =
            listOf(
                LaunchSize(375.0, 667.0),
                LaunchSize(390.0, 844.0),
                LaunchSize(402.0, 874.0),
                LaunchSize(420.0, 912.0),
                LaunchSize(440.0, 956.0),
                LaunchSize(466.0, 678.0),
                LaunchSize(280.0, 653.0),
            ) +
                DeviceProfiles.all.map { LaunchSize(it.widthDp.toDouble(), it.heightDp.toDouble()) }
        for (size in sizes) {
            for (insets in listOf(MoonTarget.Insets(0.0, 0.0, 0.0, 0.0), MoonTarget.Insets(62.0, 19.0, 34.0, 31.0))) {
                val layout = LaunchLayout(size, insets)
                val cy = size.height / 2
                assertEquals(LaunchPoint(size.width / 2, cy), layout.moonCentre)
                assertEquals(228.0, layout.sunCentre.y - cy, 0.0)
                assertEquals(48.0, layout.band.minY - cy, 0.0)
                assertEquals(size.height, layout.band.maxY, 0.0)
                assertEquals(218.0, layout.pathStart.y - cy, 0.0)
                assertEquals(layout.pathStart.y, layout.pathEnd.y, 0.0)
                assertEquals(layout.band.minY, layout.pathControl1.y, 0.0)
                assertEquals(layout.band.minY, layout.pathControl2.y, 0.0)
                assertEquals(60.0, layout.wordmarkCentre.y - cy, 0.0)
                assertEquals(96.0, layout.status.minY - cy, 0.0)
                assertEquals(insets.left, layout.safeBounds.minX, 0.0)
                assertEquals(size.width - insets.right, layout.safeBounds.maxX, 0.0)
                assertEquals(insets.top, layout.safeBounds.minY, 0.0)
                assertEquals(size.height - insets.bottom, layout.safeBounds.maxY, 0.0)
                val path = FlightPath(size)
                val board = FlightPath(LaunchSize(size.width, 844.0))
                for (ms in 500..1400 step 10) {
                    val f = FlightPath.fraction(ms / 1000.0)
                    val p = path.pointAtFraction(f)
                    val reference = board.pointAtFraction(f)
                    assertEquals(reference.x, p.x, 1e-8)
                    assertEquals(reference.y - 422, p.y - cy, 1e-8)
                }
            }
        }
    }

    @Test fun board844GeometryIsExactlyTheOriginalComposition() {
        val l = LaunchLayout(LaunchSize(390.0, 844.0))
        assertEquals(LaunchPoint(195.0, 422.0), l.moonCentre)
        assertEquals(LaunchPoint(195.0, 650.0), l.sunCentre)
        assertEquals(LaunchRect(0.0, 470.0, 390.0, 374.0), l.band)
        assertEquals(LaunchPoint(-10.0, 640.0), l.pathStart)
        assertEquals(LaunchPoint(90.0, 470.0), l.pathControl1)
        assertEquals(LaunchPoint(300.0, 470.0), l.pathControl2)
        assertEquals(LaunchPoint(400.0, 640.0), l.pathEnd)
        assertEquals(LaunchPoint(195.0, 482.0), l.wordmarkCentre)
        assertEquals(LaunchRect(0.0, 518.0, 390.0, 20.0), l.status)
    }
}
