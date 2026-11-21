package net.chronoflow.app.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsSignOutDeviceTest : ScreenTest() {
    @Test fun signOutWithNoPendingOpensSignInWithoutReplayingLaunch() {
        val started = System.nanoTime()
        val scenario = launch(settingsSeed())
        compose.waitUntilExactlyOneExists(hasTestTag("settings.signOut"), 5_000)
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ChronoFlowApp
        var frames = 0L
        var t0 = 0L
        scenario.onActivity {
            frames = checkNotNull(app.graph.launch.frameLog).count
            t0 = app.graph.launch.t0Nanos
        }
        compose.onNodeWithTag("settings.signOut").performScrollTo().performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("sign-in.scroll"), 5_000)
        compose.onNodeWithTag("sign-in.scroll").assertIsDisplayed()
        compose.onNodeWithTag("launch.opening").assertDoesNotExist()
        scenario.onActivity {
            assertEquals(frames, checkNotNull(app.graph.launch.frameLog).count)
            assertEquals(t0, app.graph.launch.t0Nanos)
            assertFalse(app.graph.launch.overlayActive)
        }
        assertTrue("settings sign-out exceeded 10 seconds", System.nanoTime() - started < 10_000_000_000L)
    }

    @Test fun pendingSignOutStayAndDiscard() {
        val started = System.nanoTime()
        launch(settingsSeed() + ("CF_SETTINGS_TEST_PENDING" to "1"))
        compose.waitUntilExactlyOneExists(hasTestTag("settings.signOut"), 5_000)
        compose.onNodeWithTag("settings.signOut").performScrollTo().performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("settings.pending"), 5_000)
        compose.onNodeWithTag("settings.pending.stay").performScrollTo().performClick()
        compose.waitUntilDoesNotExist(hasTestTag("settings.pending"), 5_000)
        compose.onNodeWithTag("settings.sync").assertExists()
        compose.onNodeWithTag("settings.signOut").performScrollTo().performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("settings.pending"), 5_000)
        compose.onNodeWithTag("settings.pending.discard").performScrollTo().performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("sign-in.scroll"), 5_000)
        compose.onNodeWithTag("sign-in.scroll").assertIsDisplayed()
        assertTrue("settings pending journey exceeded 10 seconds", System.nanoTime() - started < 10_000_000_000L)
    }
}

private fun settingsSeed() =
    mapOf(
        "CF_AUTH_TEST_SIGNED_IN" to "demo",
        "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
        // An accepted but unmapped leaf selects Settings root, as pinned iOS does.
        "CF_SETTINGS_ROUTE" to "passkeys",
        "CF_MOCK_DELAY" to "0",
    )
