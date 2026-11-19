package net.chronoflow.feature.launch.model

import net.chronoflow.core.testing.DeviceProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeGeometryTests {
    @Test fun everyDeviceUsesAllFourInsetsAndScrollsWithoutCompressingContent() {
        for (profile in DeviceProfiles.all) {
            val size = LaunchSize(profile.widthDp.toDouble(), profile.heightDp.toDouble())
            val safe =
                profile.insets.let {
                    MoonTarget.Insets(it.top.toDouble(), it.left.toDouble(), it.bottom.toDouble(), it.right.toDouble())
                }
            for (insets in listOf(safe, MoonTarget.Insets(7.0, 19.0, 11.0, 31.0))) {
                val g = WelcomeGeometry(size, insets)
                assertEquals(MoonTarget.contentBounds(size, insets), g.content)
                assertEquals(insets.left + 24, g.content.minX, 0.0)
                assertEquals(size.width - insets.right - 24, g.content.maxX, 1e-9)
                assertTrue(g.content.minY >= insets.top + 16)
                assertTrue(g.content.maxY <= size.height - insets.bottom)
                for (heading in listOf(276.0, 600.0, 1000.0)) {
                    val groups = g.groups(heading, 300.0)
                    assertTrue(groups.actions.minY >= groups.heading.maxY + 24)
                    assertTrue(groups.actions.maxY >= g.content.maxY)
                    assertEquals(heading, groups.heading.height, 0.0)
                    assertEquals(300.0, groups.actions.height, 0.0)
                }
            }
        }
    }

    @Test fun zeroInsetBoardRetainsItsComposition() {
        val g = WelcomeGeometry(LaunchSize(390.0, 844.0), MoonTarget.Insets(0.0, 0.0, 0.0, 0.0))
        assertEquals(LaunchRect(24.0, 64.0, 342.0, 740.0), g.content)
        val groups = g.groups(276.0, 296.0)
        assertEquals(64.0, groups.heading.minY, 0.0)
        assertEquals(804.0, groups.actions.maxY, 0.0)
        assertEquals(752.0, groups.actions.maxY - 52, 0.0)
        assertEquals(688.0, groups.actions.maxY - 52 - 12 - 52, 0.0)
    }
}
