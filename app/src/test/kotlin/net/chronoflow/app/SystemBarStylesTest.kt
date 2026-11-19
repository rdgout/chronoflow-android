package net.chronoflow.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemBarStylesTest {
    @Test fun everyThemeOverlayAndSkyCombinationUsesTheSpecifiedIcons() {
        for (night in listOf(false, true)) {
            for (overlay in listOf(false, true)) {
                for (sky in listOf(false, true)) {
                    val expected = if (night || overlay || sky) IconAppearance.LightIcons else IconAppearance.DarkIcons
                    assertEquals(expected, SystemBarStyles.statusBar(night, overlay, sky))
                    assertEquals(
                        if (night) IconAppearance.LightIcons else IconAppearance.DarkIcons,
                        SystemBarStyles.navigationBar(night),
                    )
                }
            }
        }
    }
}
