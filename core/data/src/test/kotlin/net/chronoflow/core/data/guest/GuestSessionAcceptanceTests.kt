package net.chronoflow.core.data.guest

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.data.wizard.MemberWizardStore
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.auth.AuthenticationMethod
import net.chronoflow.core.model.auth.GuestSessionContinuation
import net.chronoflow.core.model.launch.Gate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GuestSessionAcceptanceTests {
    @Test fun twoFactorDropsTheGuestAndContinuesToMyTripsWithoutSeeding() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAcceptanceFixture(StandardTestDispatcher(testScheduler))
            try {
                fixture.seedGuest()
                val result = fixture.result()
                fixture.controller.accept(result)
                fixture.acceptance.accept(result, AuthenticationMethod.TwoFactor, fixture.database())
                val store = MemberWizardStore(fixture.database(), now = { fixture.clock.now }, io = fixture.io)
                assertNull(store.recover())
                assertNull(fixture.guest.recover())
                assertNull(fixture.secrets.get("guest-token"))
                assertEquals(GuestSessionContinuation.Trips, fixture.continuation().consume(Gate.App))
                assertNull(fixture.continuation().consume(Gate.App))
            } finally {
                fixture.close()
            }
        }

    @Test fun continuationPersistsOnReopenAndSignOutErasesIt() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAcceptanceFixture(StandardTestDispatcher(testScheduler))
            try {
                fixture.seedGuest()
                val result = fixture.result("guest-token")
                fixture.controller.accept(result)
                fixture.acceptance.accept(result, AuthenticationMethod.SignUp, fixture.database())
                fixture.accounts.close()
                fixture.accounts.open(AccountID.of(result.account.id))
                assertEquals(GuestSessionContinuation.Trip("guest-token"), fixture.continuation().values().first())
                fixture.controller.signOut(discardPending = true)
                fixture.accounts.open(AccountID.of(result.account.id))
                assertNull(fixture.continuation().consume(Gate.App))
                assertNull(fixture.guest.recover())
                assertNull(fixture.secrets.get("guest-token"))
            } finally {
                fixture.close()
            }
        }

    @Test fun anotherAccountCannotTakeTheGuestOrItsContinuation() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAcceptanceFixture(StandardTestDispatcher(testScheduler))
            val other = AccountDatabase(fixture.root, driverFactory = JdbcAccountDriverFactory(), io = fixture.io)
            try {
                val guest = fixture.seedGuest()
                other.open(GuestAcceptanceFixture.OTHER)
                val continuation = GuestContinuationRepository(other)
                continuation.store(GuestSessionContinuation.Trips)
                try {
                    fixture.acceptance.accept(fixture.result("guest-token"), AuthenticationMethod.Password, other)
                    fail("Another account accepted the guest")
                } catch (_: GuestAcceptanceSuperseded) {
                }
                assertEquals(guest, fixture.guest.recover())
                assertEquals(GuestSessionContinuation.Trips, continuation.values().first())
                assertEquals("secret", fixture.secrets.get("guest-token"))
            } finally {
                other.close()
                fixture.close()
            }
        }

    @Test fun committedSessionFinishesContinuationAfterRequestCancellation() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAcceptanceFixture(StandardTestDispatcher(testScheduler))
            try {
                fixture.seedGuest()
                val result = fixture.result("guest-token")
                fixture.controller.accept(result)
                val routes = mutableListOf<Gate>()
                val caller =
                    launch {
                        currentCoroutineContext().cancel()
                        try {
                            fixture.acceptance.routeAcceptedSession(
                                result,
                                AuthenticationMethod.SignUp,
                                fixture.controller,
                                fixture.accounts,
                                fixture.readiness,
                                backgroundScope,
                                routes::add,
                                { fail("Unexpected verify") },
                            )
                        } catch (_: CancellationException) {
                        }
                    }
                caller.join()
                runCurrent()
                assertEquals(listOf(Gate.App), routes)
                assertEquals(GuestSessionContinuation.Trip("guest-token"), fixture.continuation().consume(Gate.App))
                assertNull(fixture.guest.recover())
            } finally {
                fixture.close()
            }
        }

    @Test fun signUpWithoutTripKeepsTheGuest() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAcceptanceFixture(StandardTestDispatcher(testScheduler))
            try {
                val guest = fixture.seedGuest()
                val result = fixture.result()
                fixture.controller.accept(result)
                fixture.acceptance.accept(result, AuthenticationMethod.SignUp, fixture.database())
                assertEquals(guest, fixture.guest.recover())
                assertEquals("secret", fixture.secrets.get("guest-token"))
                assertNull(fixture.continuation().consume(Gate.App))
            } finally {
                fixture.close()
            }
        }
}
