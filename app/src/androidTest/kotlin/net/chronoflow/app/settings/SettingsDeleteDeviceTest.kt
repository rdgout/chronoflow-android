package net.chronoflow.app.settings

import android.view.View
import android.view.inspector.WindowInspector
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.debug.RouteProbe
import net.chronoflow.debug.StrictModeViolationCounter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsDeleteDeviceTest : ScreenTest() {
    @Test fun deleteOnMockEndsOnWelcomeAndRemovesTheAccountDirectory() {
        val started = System.nanoTime()
        StrictModeViolationCounter.reset()
        val scenario =
            launch(
                mapOf(
                    "CF_AUTH_TEST_SIGNED_IN" to "demo",
                    "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                    "CF_SETTINGS_ROUTE" to "delete",
                    "CF_MOCK_DELAY" to "0",
                ),
            )
        compose.waitUntilExactlyOneExists(hasTestTag("settings.delete.confirm"), 15_000)
        compose.onNodeWithTag("settings.delete.confirm").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("settings.delete.password"), 5_000)
        compose.onNodeWithTag("settings.delete.password").performTextInput("password")
        lateinit var decor: View
        scenario.onActivity { decor = WindowInspector.getGlobalWindowViews().single { it.hasWindowFocus() } }
        waitForImeToSettle("settings.delete.submit", windowDecor = decor)
        compose.onNodeWithTag("settings.delete.submit").performScrollTo().performClick()
        compose.waitUntil(8_000) { RouteProbe.state.route == Route.Welcome }
        compose.onNodeWithTag("welcome.signIn").assertExists()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.filesDir, "uitests/${seed.testId}/ChronoFlowMock")
        assertFalse(File(root, "Accounts/0199a3c0-0000-7000-8000-000000000000").exists())
        assertFalse(File(root, "session.bin").exists())
        assertEquals(0, StrictModeViolationCounter.count)
        assertTrue("delete journey exceeded 10 seconds", System.nanoTime() - started < 10_000_000_000L)
    }
}
