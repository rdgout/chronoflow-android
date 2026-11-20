package net.chronoflow.core.data.wizard

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.wizard.support.WizardStore
import net.chronoflow.core.data.wizard.support.wizardTrip
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.testing.wizard.WizardTestClock
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.RecoveryRejected
import net.chronoflow.core.wizard.model.WizardAirportSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Enclosed::class)
class WizardRecoveryTests {
    class Recovery {
        @Test fun firstTripAndLatestOriginPrefillExcludeDeletedTrips() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardStore(StandardTestDispatcher(testScheduler))
                try {
                    val clock = WizardTestClock()
                    val store = MemberWizardStore(fixture.database, now = { clock.now }, io = fixture.io)
                    val first = store.start()
                    assertTrue(first.state.firstTrip)
                    assertFalse(first.state.entered)
                    assertFalse(first.state.fromLastTrip)
                    assertTrue(first.state.sleepNeeded)
                    store.discard(first.state.instance)
                    val trips = TripRepository(fixture.database)
                    trips.upsert(wizardTrip("old", "LHR", "2026-09-01T12:00:00Z"))
                    trips.upsert(wizardTrip("latest", "AMS", "2026-11-01T12:00:00Z"))
                    val deleted =
                        wizardTrip("deleted", "SIN", "2026-12-01T12:00:00Z")
                            .copy(deletedAt = "2026-11-01T12:00:00Z")
                    trips.upsert(deleted)
                    trips.upsert(deleted.copy(id = "local", deletedAt = null, deletedLocally = true))
                    SettingsRepository(fixture.database).upsert(SettingsRecord("sleepAnswered", "true"))
                    val next = store.start()
                    assertFalse(next.state.firstTrip)
                    assertTrue(next.state.fromLastTrip)
                    assertFalse(next.state.entered)
                    assertFalse(next.state.sleepNeeded)
                    assertEquals("AMS", next.form.legs[0].fromIata)
                    assertTrue(
                        next.form.legs[0]
                            .toIata
                            .isEmpty(),
                    )
                    assertEquals("Europe/Amsterdam", next.airports["AMS"]?.timezone)
                } finally {
                    fixture.close()
                }
            }

        @Test fun discardSaveAndExpiredRecordCannotBeResurrectedByAnOldWrite() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardStore(StandardTestDispatcher(testScheduler))
                try {
                    val clock = WizardTestClock()
                    val store = MemberWizardStore(fixture.database, now = { clock.now }, io = fixture.io)
                    val initial = store.start()
                    val first = initial.copy(state = initial.state.copy(revision = 3))
                    store.persist(first)
                    val older =
                        first.copy(
                            state = first.state.copy(revision = 1),
                            form = first.form.copy(legs = listOf(first.form.legs[0].copy(toIata = "WRONG"))),
                        )
                    store.persist(older)
                    assertEquals(first, store.recover())
                    store.discard(first.state.instance)
                    assertRejected { store.persist(first) }
                    val second = store.start()
                    store.discard(first.state.instance)
                    assertEquals(second, store.recover())
                    store.didSave(second.state.instance, second.state.firstTrip)
                    assertNull(store.recover())
                    val started = store.start()
                    val saved = started.copy(state = started.state.copy(savedTripId = "saved"))
                    store.persist(saved)
                    assertEquals("saved", store.recover()?.state?.savedTripId)
                    store.didSave(saved.state.instance, saved.state.firstTrip)
                    val expired = store.start()
                    clock.advance(1800.0)
                    assertNull(store.recover())
                    assertRejected { store.persist(expired) }
                } finally {
                    fixture.close()
                }
            }
    }

    @RunWith(Parameterized::class)
    class Expiry(
        private val elapsed: Double,
    ) {
        @Test fun recoveryExpiresAtExactlyThirtyMinutes() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardStore(StandardTestDispatcher(testScheduler))
                try {
                    val clock = WizardTestClock()
                    val store = MemberWizardStore(fixture.database, now = { clock.now }, io = fixture.io)
                    val initial = store.start()
                    val draft =
                        initial.copy(
                            state =
                                initial.state.copy(
                                    step = MemberWizardStep.outbound,
                                    entered = true,
                                    revision = 1,
                                    sheet = WizardAirportSheet(0, WizardAirportSheet.Side.to),
                                ),
                            form =
                                initial.form.copy(
                                    legs = listOf(initial.form.legs[0].copy(departLocal = "2027-03-28T02:30")),
                                ),
                        )
                    store.persist(draft)
                    val repository = DraftRepository(fixture.database)
                    assertTrue(repository.drafts().isEmpty())
                    assertTrue(repository.values().first().isEmpty())
                    clock.advance(elapsed)
                    val recovered = MemberWizardStore(fixture.database, now = { clock.now }, io = fixture.io).recover()
                    assertEquals(if (elapsed < 1800) draft else null, recovered)
                    assertEquals(elapsed >= 1800, repository.recovery(clock.now) == null)
                } finally {
                    fixture.close()
                }
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "elapsed={0}s")
            fun elapsed(): List<Array<Any>> = listOf(0.0, 1799.0, 1799.999, 1800.0, 1801.0).map { arrayOf(it) }
        }
    }
}

internal suspend fun assertRejected(block: suspend () -> Unit) {
    try {
        block()
        error("Expected rejected recovery")
    } catch (_: RecoveryRejected) {
    }
}
