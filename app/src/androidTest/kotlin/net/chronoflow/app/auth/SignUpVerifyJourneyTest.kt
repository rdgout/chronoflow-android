package net.chronoflow.app.auth

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.ioViolationsForTest
import net.chronoflow.app.resetIoViolationsForTest
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.debug.RouteProbe
import org.junit.Assert.assertEquals
import org.junit.Test

@Smoke
@OptIn(ExperimentalTestApi::class)
class SignUpVerifyJourneyTest : ScreenTest() {
    @Test fun signUpVerifiesThroughTheMockLinkAndOpensTheGate() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val graph = (instrumentation.targetContext.applicationContext as ChronoFlowApp).graph
        instrumentation.runOnMainSync { graph.resetIoViolationsForTest() }
        launch(
            mapOf(
                "CF_AUTH_TEST_AUDIT_SCREEN" to "sign-up",
                "CF_MOCK_DELAY" to "0",
            ),
        )
        compose.waitUntilExactlyOneExists(hasTestTag("sign-up.name"), 15_000)
        compose.onNodeWithTag("sign-up.name").performTextInput("Traveller")
        compose.onNodeWithTag("sign-up.name").performImeAction()
        compose.onNodeWithTag("sign-up.email").assertIsFocused()
        compose.onNodeWithTag("sign-up.email").performTextInput("new@example.com")
        compose.onNodeWithTag("sign-up.email").performImeAction()
        compose.onNodeWithTag("sign-up.password").assertIsFocused()
        compose.onNodeWithTag("sign-up.password").performTextInput("NewPassword123!")
        compose.onNodeWithTag("sign-up.password").performImeAction()
        compose.onNodeWithTag("sign-up.confirmation").assertIsFocused()
        compose.onNodeWithTag("sign-up.confirmation").performTextInput("NewPassword123!")
        compose.onNodeWithTag("sign-up.confirmation").performImeAction()
        waitFor(15_000) { RouteProbe.state.route == Route.Verify }
        compose.waitUntilExactlyOneExists(hasTestTag("verify.open-email") and isEnabled(), 15_000)
        compose.onNodeWithTag("verify.open-email").performClick()
        waitFor(15_000) { RouteProbe.state.route == Route.Walkthrough() }
        compose.onNodeWithTag("verify.open-email").assertDoesNotExist()
        instrumentation.runOnMainSync { assertEquals(0, graph.ioViolationsForTest) }
    }
}
