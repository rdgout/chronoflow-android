package net.chronoflow.app.guest

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.data.wizard.MemberWizardStore
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.core.model.auth.GuestSessionContinuation
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.feature.auth.model.SignUpModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class GuestAcceptanceWiringTest {
    @Test fun signUpWithALiveClaimStoresTheTripAndClearsTheGuest() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAuthFixture(this)
            try {
                val token = fixture.seedGuestReview()
                val model = fixture.auth.signIn.makeSignUpModel {}
                model.editName("Traveller")
                model.editEmail("guest-journey@example.com")
                assertEquals(
                    SignUpModel.Outcome.Accepted,
                    model.submit(
                        GuestTestSecret(),
                        GuestTestSecret(),
                        AuthLanguage.En,
                        requiresGuestClaim = true,
                    ) {},
                )
                assertEquals(Route.Verify, fixture.navigator.state.value.route)
                assertEquals(GuestSessionContinuation.Trip(token), fixture.continuation().consume(Gate.App))
                assertFalse(fixture.file.exists())
                assertNull(fixture.secrets.get(token))
            } finally {
                fixture.close()
            }
        }

    @Test fun passwordSignInWithALiveDraftSeedsTheMemberWizard() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAuthFixture(this)
            try {
                fixture.seedGuestReview()
                fixture.auth.signIn.editEmail("demo@example.com")
                fixture.auth.signIn.submitPassword(GuestTestSecret("password")) {}
                assertEquals(Route.Today, fixture.navigator.state.value.route)
                val store = MemberWizardStore(fixture.database(), now = { fixture.clock.now }, io = fixture.dispatcher)
                assertEquals(MemberWizardStep.checking, store.recover()?.state?.step)
                assertEquals(GuestSessionContinuation.MemberWizard, fixture.continuation().consume(Gate.App))
                assertFalse(fixture.file.exists())
            } finally {
                fixture.close()
            }
        }

    @Test fun signInWithoutAGuestLeavesEverythingAsBefore() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAuthFixture(this)
            try {
                fixture.auth.signIn.editEmail("demo@example.com")
                fixture.auth.signIn.submitPassword(GuestTestSecret("password")) {}
                assertEquals(Route.Today, fixture.navigator.state.value.route)
                assertNull(fixture.continuation().consume(Gate.App))
                assertNull(fixture.guest.recover())
            } finally {
                fixture.close()
            }
        }
}
