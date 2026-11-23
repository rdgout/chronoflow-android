package net.chronoflow.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.debug.RouteProbe
import org.junit.Test

@Smoke
@OptIn(ExperimentalTestApi::class)
class OnboardingJourneyTest : ScreenTest() {
    @Test fun acceptTermsThenWalkthroughOpensToday() {
        launch(
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_TERMS_PENDING" to "1",
                "CF_WALKTHROUGH_TEST_PENDING" to "1",
                "cf.settings.language" to "en",
            ),
        )
        compose.waitUntilExactlyOneExists(hasTestTag("terms.screen"), 10_000)
        compose.onNodeWithTag("terms.accept").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("walkthrough.screen"), 10_000)
        compose.onNodeWithTag("walkthrough.next").performClick()
        compose.onNodeWithTag("walkthrough.next").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("walkthrough.done"), 10_000)
        compose.onNodeWithTag("walkthrough.done").performClick()
        compose.waitUntil(10_000) { RouteProbe.state.route == Route.Today }
        compose.onNodeWithTag("walkthrough.screen").assertDoesNotExist()
    }
}
