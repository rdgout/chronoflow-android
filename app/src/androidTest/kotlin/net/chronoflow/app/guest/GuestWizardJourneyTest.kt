package net.chronoflow.app.guest

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.ioViolationsForTest
import net.chronoflow.app.resetIoViolationsForTest
import net.chronoflow.app.shell.ShellRouteProbe
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.model.guest.GuestRecoveryLocation
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.debug.GuestWizardDebugSecrets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class GuestWizardJourneyTest : ScreenTest() {
    @Test fun testWelcomeThroughSignupOpensClaimedTrip() {
        val started = System.nanoTime()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as ChronoFlowApp
        instrumentation.runOnMainSync { app.graph.resetIoViolationsForTest() }
        launch(
            mapOf(
                "CF_MOCK_DELAY" to "0",
                "cf.settings.language" to null,
                "cf.settings.theme" to null,
                "CF_GUEST_WIZARD" to "1",
                "CF_GUEST_WIZARD_JOURNEY" to "1",
                "CF_WIZARD_BOARD_CAPTURE" to "WizardGuestJourney",
            ),
        )
        tap("wizard.sleep.continue")
        tap("wizard.choice.question.18_64")
        tap("wizard.fine-tune.continue")
        tap("wizard.ready.continue", 3_000)
        compose.waitUntil(2_000) { compose.onNodeWithTag("guest-signup.name").isDisplayed() }
        val graph = app.graph
        val token =
            runBlocking(graph.dispatchers.io) {
                val token =
                    checkNotNull(
                        graph.guestWizardStore
                            .recover()
                            ?.form
                            ?.identity
                            ?.token,
                    )
                assertTrue(
                    GuestWizardDebugSecrets.hasReviewSecret(
                        graph.guestWizardStore,
                        graph.guestSecrets,
                        graph.dispatchers.io,
                    ),
                )
                token
            }
        compose.onNodeWithTag("guest-signup.submit").performScrollTo().performClick()
        tap("verify.open-email")
        tap("walkthrough.skip")
        compose.waitUntil(3_000) { compose.onNodeWithTag("trip.scroll").isDisplayed() }
        compose.onNodeWithTag("wizard.ready.continue").assertDoesNotExist()
        runBlocking(graph.dispatchers.io) {
            assertFalse(GuestRecoveryLocation.file(graph.storageRoot(), graph.namespace).exists())
            assertNull(graph.guestSecrets.get(token))
        }
        instrumentation.runOnMainSync {
            assertEquals(token, (ShellRouteProbe.route as? Route.TripPage)?.tripId)
            assertEquals(0, app.graph.ioViolationsForTest)
        }
        assertTrue("guest journey exceeded 10 s", System.nanoTime() - started < 10_000_000_000L)
    }

    private fun tap(
        tag: String,
        timeout: Long = 5_000,
    ) {
        compose.waitUntil(timeout) { compose.onNode(hasTestTag(tag) and isEnabled()).isDisplayed() }
        compose.onNodeWithTag(tag).performClick()
    }
}
