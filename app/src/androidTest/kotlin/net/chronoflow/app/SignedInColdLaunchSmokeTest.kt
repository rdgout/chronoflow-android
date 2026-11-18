package net.chronoflow.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.debug.RouteProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@Smoke
@RunWith(AndroidJUnit4::class)
class SignedInColdLaunchSmokeTest : ScreenTest() {
    @Test fun signedInColdLaunchReachesToday() {
        val started = System.nanoTime()
        launch(
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_UI_TEST_PROFILE" to "launch",
            ),
        )
        compose.waitUntilExactlyOneExists(hasTestTag("route.today"), 10_000)
        assertEquals(Route.Today, RouteProbe.state.route)
        assertTrue((System.nanoTime() - started) < 10_000_000_000L)
    }
}
