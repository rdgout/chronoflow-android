package net.chronoflow.app.auth

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
class TwoFactorJourneyTest : ScreenTest() {
    @Test fun numberPadDoesNotAutoSubmitAndVerifyCompletesTheMockSession() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val graph = (instrumentation.targetContext.applicationContext as ChronoFlowApp).graph
        instrumentation.runOnMainSync { graph.resetIoViolationsForTest() }
        launch(twoFactorScreenSeed)
        compose.waitUntilExactlyOneExists(hasTestTag("two-factor.code"), 5_000)
        compose.onNodeWithTag("two-factor.code").performTextInput("1234567")
        assertEquals(
            "123456",
            compose
                .onNodeWithTag("two-factor.code")
                .fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .text,
        )
        assertEquals(Route.TwoFactor, RouteProbe.state.route)
        waitForImeToSettle("two-factor.submit")
        compose.onNodeWithTag("two-factor.submit").performScrollTo().performClick()
        waitFor(5_000) { RouteProbe.state.route == Route.Today }
        compose.onNodeWithTag("two-factor.code").assertDoesNotExist()
        instrumentation.runOnMainSync { assertEquals(0, graph.ioViolationsForTest) }
    }
}

internal val twoFactorScreenSeed =
    mapOf(
        "CF_AUTH_TEST_AUDIT_SCREEN" to "two-factor",
        "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
        "CF_MOCK_DELAY" to "0",
    )
