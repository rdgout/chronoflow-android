package net.chronoflow.app

import androidx.compose.ui.test.hasTestTag
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

@Smoke
class ScreenTestHarnessTest : ScreenTest() {
    @Test
    fun seededLaunchShowsTheRootInDutchWithItsTestDirectory() {
        val scenario = launch(mapOf("cf.settings.language" to "nl"))
        compose.onNode(hasTestTag("app.root")).assertExists()
        var language = ""
        scenario.onActivity {
            language =
                it.resources.configuration.locales[0]
                    .language
        }
        assertEquals("nl", language)
        val directory =
            File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "uitests/${seed.testId}")
        assertTrue(directory.isDirectory)
    }
}
