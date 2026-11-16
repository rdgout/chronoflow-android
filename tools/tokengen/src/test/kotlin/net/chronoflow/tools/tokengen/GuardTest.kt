package net.chronoflow.tools.tokengen

import org.junit.Assert.assertThrows
import org.junit.Test

class GuardTest {
    @Test
    fun pressAndSplashGuardsRejectBoardDrift() {
        val theme = repoRoot.resolve(THEME_PATH).readText()
        assertThrows(GenerationError::class.java) { pressTokens(theme.replaceFirst("scale(0.97)", "scale(0.96)")) }
        val splash = repoRoot.design("SplashScreen.dc.html")
        assertThrows(
            GenerationError::class.java,
        ) { launchPaint(splash.replace("background: #fff;", "background: #fefefe;")) }
    }
}
