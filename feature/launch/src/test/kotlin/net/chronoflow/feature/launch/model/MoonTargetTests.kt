package net.chronoflow.feature.launch.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoonTargetTests {
    private val size = LaunchSize(390.0, 844.0)
    private val insets = MoonTarget.Insets(47.0, 0.0, 34.0, 0.0)

    @Test fun boardTargetAndPaddingRule() {
        val target = MoonTarget.fallback(size, insets, 28.0)
        assertEquals(LaunchRect(24.0, 64.0, 28.0, 28.0), target)
        assertEquals(38.0, target.midX, 0.0)
        assertEquals(78.0, target.midY, 0.0)
        assertEquals(LaunchRect(24.0, 64.0, 342.0, 740.0), MoonTarget.contentBounds(size, insets))
    }

    @Test fun largerRowMovesTheCentreAndSmallRowsKeepTheMoonSize() {
        for ((row, centre) in listOf(20.0 to 78.0, 28.0 to 78.0, 40.0 to 84.0, 56.0 to 92.0, 96.0 to 112.0)) {
            val target = MoonTarget.fallback(size, insets, row)
            assertEquals(centre, target.midY, 0.0)
            assertEquals(28.0, target.width, 0.0)
            assertEquals(28.0, target.height, 0.0)
        }
    }

    @Test fun allFourSafeInsetsDefineTheContent() {
        val size = LaunchSize(590.0, 900.0)
        val insets = MoonTarget.Insets(62.0, 36.0, 54.0, 22.0)
        val content = MoonTarget.contentBounds(size, insets)
        assertEquals(LaunchRect(60.0, 78.0, 484.0, 768.0), content)
        val target = MoonTarget.fallback(size, insets, 40.0)
        assertEquals(74.0, target.midX, 0.0)
        assertEquals(98.0, target.midY, 0.0)
        assertTrue(content.contains(target))
    }

    @Test fun shortAndWideWindowsAndInsetsRemainInputs() {
        for (size in listOf(LaunchSize(375.0, 667.0), LaunchSize(402.0, 874.0), LaunchSize(590.0, 900.0))) {
            for (insets in listOf(MoonTarget.Insets(20.0, 0.0, 0.0, 0.0), MoonTarget.Insets(62.0, 32.0, 34.0, 18.0))) {
                val target = MoonTarget.fallback(size, insets, 28.0)
                assertEquals(insets.left + 38, target.midX, 0.0)
                assertEquals(maxOf(insets.top + 16, 64.0) + 14, target.midY, 0.0)
                assertTrue(MoonTarget.contentBounds(size, insets).contains(target))
            }
        }
    }
}
