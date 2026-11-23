package net.chronoflow.core.data.guest

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.auth.AuthenticationMethod
import net.chronoflow.core.model.auth.GuestSessionContinuation
import net.chronoflow.core.model.launch.Gate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class GuestSessionAcceptanceClaimedTripTests(
    private val method: AuthenticationMethod,
) {
    @Test fun claimedTripSurvivesEveryGateAndIsConsumedOnce() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAcceptanceFixture(StandardTestDispatcher(testScheduler))
            try {
                fixture.seedGuest()
                val result = fixture.result("guest-token", verified = false, accepted = false, seen = false)
                fixture.controller.accept(result)
                val routes = mutableListOf<Gate>()
                fixture.acceptance.routeAcceptedSession(
                    result,
                    method,
                    fixture.controller,
                    fixture.accounts,
                    fixture.readiness,
                    backgroundScope,
                    routes::add,
                    { routes.add(Gate.Verify) },
                )
                assertEquals(listOf(Gate.Verify), routes)
                assertNull(fixture.guest.recover())
                assertNull(fixture.secrets.get("guest-token"))
                checkGate(fixture, Gate.Verify, routes)
                checkGate(fixture, Gate.Terms, routes)
                checkGate(fixture, Gate.Walkthrough, routes)
                checkGate(fixture, Gate.App, routes)
                assertNull(fixture.continuation().consume(Gate.App))
            } finally {
                fixture.close()
            }
        }

    private suspend fun checkGate(
        fixture: GuestAcceptanceFixture,
        gate: Gate,
        routes: MutableList<Gate>,
    ) {
        val account =
            fixture
                .result(
                    verified = gate != Gate.Verify,
                    accepted = gate == Gate.Walkthrough || gate == Gate.App,
                    seen = gate == Gate.App,
                ).account
        fixture.accounts.upsert(account)
        fixture.readiness.routeAcceptedSession(routes::add) { routes.add(Gate.Verify) }
        assertEquals(gate, routes.last())
        val expected = if (gate == Gate.App) GuestSessionContinuation.Trip("guest-token") else null
        assertEquals(expected, fixture.continuation().consume(gate))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "method={0}")
        fun methods(): List<AuthenticationMethod> =
            listOf(
                AuthenticationMethod.SignUp,
                AuthenticationMethod.Password,
                AuthenticationMethod.Passkey,
            )
    }
}
