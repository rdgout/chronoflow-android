@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.wizard.model

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.wizard.model.WizardFlightTiming.Field
import net.chronoflow.core.wizard.wizardAirport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

internal val timingAirports =
    mapOf(
        "AMS" to "Europe/Amsterdam",
        "JFK" to "America/New_York",
        "NRT" to "Asia/Tokyo",
        "HNL" to "Pacific/Honolulu",
        "AKL" to "Pacific/Auckland",
        "LAX" to "America/Los_Angeles",
        "SFO" to "America/Los_Angeles",
    ).mapValues { (code, zone) -> wizardAirport(code, code, zone) }

internal fun timingRoute(vararg codes: String): WizardForm =
    codes.toList().zipWithNext().map { (from, to) -> WizardLeg(fromIata = from, toIata = to) }.let {
        WizardForm(legs = it, stays = it.map { WizardStay() })
    }

private val timingNow = Instant.parse("2035-06-01T00:00:00Z")

@RunWith(Enclosed::class)
class WizardFlightTimingTests {
    @RunWith(Parameterized::class)
    class Arrival(
        private val from: String,
        private val to: String,
        private val departure: String,
        private val minimum: String,
        private val arrival: String,
    ) {
        @Test fun arrivalBoundsAndEmptyStartConvertInstants() {
            var form = timingRoute(from, to).updateLeg(0) { it.copy(departLocal = departure) }
            val timing = WizardFlightTiming(form, timingAirports)
            assertEquals(minimum, timing.minimumLocal(Field.arrival(0))?.let(WizardDateTime::string))
            assertEquals(minimum, WizardDateTime.string(timing.initialLocal(Field.arrival(0), timingNow)))
            assertNull(timing.minimumLocal(Field.departure(0)))
            form = form.updateLeg(0) { it.copy(arriveLocal = arrival) }
            assertTrue(WizardOutboundRules.errors(form, timingAirports, timingNow, false).isEmpty())
            assertEquals(
                arrival,
                WizardDateTime.string(
                    WizardFlightTiming(form, timingAirports).initialLocal(Field.arrival(0), timingNow),
                ),
            )
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}-{1}")
            fun cases() =
                listOf(
                    arrayOf("NRT", "HNL", "2026-11-12T21:00", "2026-11-12T02:00", "2026-11-12T09:00"),
                    arrayOf("AKL", "LAX", "2026-11-12T08:00", "2026-11-11T11:00", "2026-11-11T21:00"),
                    arrayOf("AMS", "JFK", "2026-11-12T14:00", "2026-11-12T08:00", "2026-11-12T16:00"),
                    arrayOf("JFK", "AMS", "2026-11-12T21:00", "2026-11-13T03:00", "2026-11-13T10:00"),
                    arrayOf("NRT", "SFO", "2026-11-12T17:00", "2026-11-12T00:00", "2026-11-12T10:00"),
                )
        }
    }

    class Basics {
        @Test fun unsetPredecessorHasNoBoundButStartFindsNearestEarlierSetField() {
            var form =
                timingRoute(
                    "NRT",
                    "HNL",
                    "LAX",
                    "JFK",
                ).let { it.copy(end = WizardEnd(kind = "return_legs", returnFromLeg = 2)) }
                    .updateLeg(0) { it.copy(departLocal = "2026-11-12T21:00", arriveLocal = "2026-11-12T09:00") }
            var timing = WizardFlightTiming(form, timingAirports)
            assertNull(timing.minimumLocal(Field.arrival(1)))
            assertEquals("2026-11-12T11:00", WizardDateTime.string(timing.initialLocal(Field.arrival(1), timingNow)))
            assertNull(timing.minimumLocal(Field.departure(2)))
            assertEquals("2026-11-12T11:00", WizardDateTime.string(timing.initialLocal(Field.departure(2), timingNow)))
            form = form.updateLeg(0) { it.copy(arriveLocal = "") }
            timing = WizardFlightTiming(form, timingAirports)
            assertNull(timing.minimumLocal(Field.departure(1)))
            assertEquals("2026-11-12T02:00", WizardDateTime.string(timing.initialLocal(Field.departure(1), timingNow)))
        }

        @Test fun noEarlierValuesUsesAirportLocalNowToTheMinuteAndIgnoresLaterValues() {
            var form = timingRoute("NRT", "HNL")
            val timing = WizardFlightTiming(form, timingAirports)
            val now = timingNow.plusSeconds(37)
            assertNull(timing.minimumLocal(Field.arrival(0)))
            assertEquals("2035-06-01T09:00", WizardDateTime.string(timing.initialLocal(Field.departure(0), now)))
            assertEquals("2035-05-31T14:00", WizardDateTime.string(timing.initialLocal(Field.arrival(0), now)))
            assertEquals(0, timing.initialLocal(Field.departure(0), now).second)
            assertEquals(37, WizardFlightTiming(form, emptyMap()).initialLocal(Field.departure(0), now).second)
            form = form.updateLeg(0) { it.copy(arriveLocal = "2040-01-01T10:00") }
            assertEquals(
                "2035-06-01T09:00",
                WizardDateTime.string(
                    WizardFlightTiming(form, timingAirports).initialLocal(Field.departure(0), timingNow),
                ),
            )
            assertNull(WizardFlightTiming(form, timingAirports).minimumLocal(Field.departure(0)))
        }

        @Test fun nonexistentDSTSourceOrMissingZoneCannotEstablishABound() {
            var form = timingRoute("AMS", "JFK").updateLeg(0) { it.copy(departLocal = "2027-03-28T02:30") }
            assertNull(WizardFlightTiming(form, timingAirports).minimumLocal(Field.arrival(0)))
            form = form.updateLeg(0) { it.copy(departLocal = "2027-03-28T03:30") }
            assertNull(WizardFlightTiming(form, timingAirports - "AMS").minimumLocal(Field.arrival(0)))
            assertNull(WizardFlightTiming(form, timingAirports - "JFK").minimumLocal(Field.arrival(0)))
        }

        @Test fun connectionValidationComparesInstantsThroughEachAirportZone() {
            var form =
                timingRoute("NRT", "HNL", "LAX")
                    .updateLeg(0) { it.copy(arriveLocal = "2026-11-12T09:00") }
                    .updateLeg(1) { it.copy(fromIata = "LAX", departLocal = "2026-11-12T10:00") }
            assertEquals(
                "2026-11-12T11:00",
                WizardFlightTiming(form, timingAirports).minimumLocal(Field.departure(1))?.let(WizardDateTime::string),
            )
            assertEquals(
                "This flight leaves before the previous one lands.",
                WizardValidation.validate(form, timingAirports)["legs.1.departLocal"]?.key,
            )
            form = form.updateLeg(1) { it.copy(departLocal = "2026-11-12T12:00") }
            assertNull(WizardValidation.validate(form, timingAirports)["legs.1.departLocal"])
        }
    }

    @RunWith(Parameterized::class)
    class DestinationDST(
        private val departure: String,
        private val minimum: String,
    ) {
        @Test fun destinationDSTOffsetComesFromThePredecessorInstant() {
            val timing =
                WizardFlightTiming(
                    timingRoute("NRT", "AMS").updateLeg(0) { it.copy(departLocal = departure) },
                    timingAirports,
                )
            assertEquals(minimum, timing.minimumLocal(Field.arrival(0))?.let(WizardDateTime::string))
            assertEquals(minimum, WizardDateTime.string(timing.initialLocal(Field.arrival(0), timingNow)))
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun cases() =
                listOf(
                    arrayOf("2027-03-28T09:30", "2027-03-28T01:30"),
                    arrayOf("2027-03-28T10:30", "2027-03-28T03:30"),
                    arrayOf("2027-10-31T08:30", "2027-10-31T01:30"),
                    arrayOf("2027-10-31T12:30", "2027-10-31T04:30"),
                )
        }
    }

    @RunWith(Parameterized::class)
    class SourceDST(
        private val departure: String,
    ) {
        @Test fun sourceDSTResolutionMatchesExistingValidation() {
            val form = timingRoute("AMS", "JFK").updateLeg(0) { it.copy(departLocal = departure) }
            val instant = WizardDateTime.instant(departure, "Europe/Amsterdam")!!
            val local = WizardFlightTiming(form, timingAirports).minimumLocal(Field.arrival(0))!!
            assertEquals(instant, WizardDateTime.instant(WizardDateTime.string(local), "America/New_York"))
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun cases() = listOf("2027-03-28T01:30", "2027-03-28T03:30", "2027-10-31T02:30", "2027-10-31T03:30")
        }
    }

    @RunWith(Parameterized::class)
    class Chains(
        private val field: Field,
        private val expected: String,
    ) {
        @Test fun boundsChainThroughOutboundAndEveryHomeFlight() {
            var form = timingRoute("NRT", "HNL", "LAX", "JFK", "AMS")
            val values =
                listOf(
                    "2026-11-12T21:00" to "2026-11-12T09:00",
                    "2026-11-12T12:00" to "2026-11-12T20:00",
                    "2026-11-20T10:00" to "2026-11-20T18:00",
                    "2026-11-20T21:00" to "2026-11-21T10:00",
                )
            form =
                form.copy(
                    end = WizardEnd(kind = "return_legs", returnFromLeg = 2),
                    legs =
                        form.legs.mapIndexed {
                            i,
                            leg,
                            ->
                            leg.copy(departLocal = values[i].first, arriveLocal = values[i].second)
                        },
                )
            assertTrue(WizardFlightRules.errors(form, timingAirports, timingNow, false).isEmpty())
            val invalid = form.updateLeg(1) { it.copy(departLocal = "2026-11-12T08:59") }
            assertEquals(
                "This flight leaves before the previous one lands.",
                WizardOutboundRules.errors(invalid, timingAirports, timingNow, false)["legs.1.departLocal"]?.key,
            )
            form =
                form.updateLeg(
                    field.index,
                ) { if (field.isArrival) it.copy(arriveLocal = "") else it.copy(departLocal = "") }
            val timing = WizardFlightTiming(form, timingAirports)
            assertEquals(expected, timing.minimumLocal(field)?.let(WizardDateTime::string))
            assertEquals(expected, WizardDateTime.string(timing.initialLocal(field, timingNow)))
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "field={0}")
            fun cases() =
                listOf(
                    arrayOf(Field.departure(1), "2026-11-12T09:00"),
                    arrayOf(Field.departure(2), "2026-11-12T20:00"),
                    arrayOf(Field.departure(3), "2026-11-20T18:00"),
                    arrayOf(Field.arrival(2), "2026-11-20T13:00"),
                )
        }
    }

    class ChangedTimes {
        @Test fun changedEarlierValuesKeepStoredTimesAndValidationFlagsThem() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                val model = fixture.model
                val values =
                    listOf(
                        "2026-11-12T14:00" to "2026-11-12T16:00",
                        "2026-11-13T10:00" to "2026-11-14T00:00",
                        "2026-11-14T10:00" to "2026-11-15T10:00",
                    )
                val form = timingRoute("AMS", "JFK", "AMS", "NRT")
                model.form =
                    form.copy(
                        end = WizardEnd(kind = "return_legs", returnFromLeg = 1),
                        legs =
                            form.legs.mapIndexed {
                                i,
                                leg,
                                ->
                                leg.copy(departLocal = values[i].first, arriveLocal = values[i].second)
                            },
                    )
                model.state = model.state.copy(airports = timingAirports)
                model.setDateTime("2026-11-13T01:00", 0, false)
                assertEquals("2026-11-12T19:00", model.minimumLocal(Field.arrival(0))?.let(WizardDateTime::string))
                assertEquals(values[0].second, WizardDateTime.string(model.initialLocal(Field.arrival(0))))
                assertFalse(model.finishOutbound())
                assertEquals(
                    "Check the departure and arrival times.",
                    model.draft.fieldErrors["legs.0.arriveLocal"]?.key,
                )
                model.setDateTime("2026-11-13T12:00", 0, true)
                assertEquals(values[1].first, WizardDateTime.string(model.initialLocal(Field.departure(1))))
                assertFalse(model.finishReturn())
                assertEquals(
                    "This flight leaves before the previous one lands.",
                    model.draft.fieldErrors["legs.1.departLocal"]?.key,
                )
                model.setDateTime("2026-11-14T12:00", 1, true)
                assertEquals(values[2].first, WizardDateTime.string(model.initialLocal(Field.departure(2))))
                assertFalse(model.finishReturn())
                assertEquals(
                    "This flight leaves before the previous one lands.",
                    model.draft.fieldErrors["legs.2.departLocal"]?.key,
                )
                assertEquals(values[1].first, model.form.legs[1].departLocal)
                assertEquals(values[2].first, model.form.legs[2].departLocal)
                model.flush()
                assertEquals(model.form, fixture.store.last?.form)
                fixture.close()
            }
    }
}
