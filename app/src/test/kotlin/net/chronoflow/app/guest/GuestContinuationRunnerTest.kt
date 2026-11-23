package net.chronoflow.app.guest

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.auth.GuestSessionContinuation
import net.chronoflow.core.model.launch.Gate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class GuestContinuationRunnerTest {
    @Test fun tripOpensOnceAtTheAppGate() =
        runTest(timeout = 60.seconds) {
            val trip = GuestSessionContinuation.Trip("t1")
            val fixture = Fixture(trip)
            fixture.runner(values = { flowOf(trip, trip) }).run()
            assertEquals(listOf("t1"), fixture.trips)
        }

    @Test fun tripsSelectsMyTrips() =
        runTest(timeout = 60.seconds) {
            val fixture = Fixture(GuestSessionContinuation.Trips)
            fixture.runner().run()
            assertEquals(1, fixture.myTrips)
        }

    @Test fun memberWizardOpensTheWizard() =
        runTest(timeout = 60.seconds) {
            val fixture = Fixture(GuestSessionContinuation.MemberWizard)
            fixture.runner().run()
            assertEquals(1, fixture.wizards)
        }

    @Test fun nonAppGateConsumesNothing() =
        runTest(timeout = 60.seconds) {
            val trip = GuestSessionContinuation.Trip("t1")
            val fixture = Fixture(trip)
            fixture.runner(gate = { Gate.Verify }).run()
            assertTrue(fixture.trips.isEmpty())
            assertEquals(trip, fixture.row)
        }

    @Test fun stopsWhenTheSessionChanges() =
        runTest(timeout = 60.seconds) {
            val fixture = Fixture(GuestSessionContinuation.Trip("t1"))
            var continued = false
            fixture
                .runner(values = {
                    flow {
                        emit(fixture.row)
                        continued = true
                    }
                }, isCurrent = { false })
                .run()
            assertFalse(continued)
            assertTrue(fixture.trips.isEmpty())
            assertEquals(0, fixture.consumes)
        }

    @Test fun staleAfterTheGateSkipsOnlyThatValue() =
        runTest(timeout = 60.seconds) {
            val trip = GuestSessionContinuation.Trip("t1")
            val fixture = Fixture(trip)
            val current = mutableListOf(true, false, true, true)
            var gates = 0
            fixture
                .runner(values = { flowOf(trip, trip) }, isCurrent = { current.removeAt(0) }, gate = {
                    gates++
                    Gate.App
                })
                .run()
            assertEquals(2, gates)
            assertEquals(1, fixture.consumes)
            assertEquals(listOf("t1"), fixture.trips)
        }

    @Test fun storeErrorEndsQuietly() =
        runTest(timeout = 60.seconds) {
            val fixture = Fixture(GuestSessionContinuation.Trip("t1"))
            fixture.runner(values = { flow { throw IOException() } }).run()
            assertEquals(0, fixture.consumes)
            assertTrue(fixture.trips.isEmpty())
        }

    private class Fixture(
        var row: GuestSessionContinuation?,
    ) {
        val trips = mutableListOf<String>()
        var myTrips = 0
        var wizards = 0
        var consumes = 0

        fun runner(
            values: () -> Flow<GuestSessionContinuation?> = { flowOf(row) },
            gate: suspend () -> Gate = { Gate.App },
            isCurrent: suspend () -> Boolean = { true },
        ) = GuestContinuationRunner(values, consume = { gate ->
            consumes++
            if (gate == Gate.App) row.also { row = null } else null
        }, gate, isCurrent, trips::add, { myTrips++ }, { wizards++ })
    }
}
