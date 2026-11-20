package net.chronoflow.core.wizard.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.TimeZone

@RunWith(Enclosed::class)
class WizardPickerDatesTest {
    @RunWith(Parameterized::class)
    class Zones(
        private val zone: String,
    ) {
        @Test fun utcMillisRoundTripInEveryDeviceZone() {
            val old = TimeZone.getDefault()
            try {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                val first = LocalDate.of(2026, 10, 12)
                val second = LocalDate.of(2040, 3, 25)
                assertEquals(1791763200000L, WizardPickerDates.utcMillis(first))
                assertEquals(2216246400000L, WizardPickerDates.utcMillis(second))
                assertEquals(first, WizardPickerDates.localDate(1791763200000L))
                assertEquals(second, WizardPickerDates.localDate(2216246400000L))
            } finally {
                TimeZone.setDefault(old)
            }
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun cases() = listOf("Pacific/Kiritimati", "Etc/GMT+12", "Europe/Amsterdam", "UTC")
        }
    }

    class Inputs {
        @Test fun nativePickerReceivesOnlyTheAirportLocalLowerBound() {
            val minimum = LocalDateTime.of(2040, 3, 25, 16, 7)
            assertFalse(
                WizardPickerDates.isSelectable(
                    WizardPickerDates.utcMillis(minimum.toLocalDate().minusDays(1)),
                    minimum,
                ),
            )
            assertTrue(WizardPickerDates.isSelectable(WizardPickerDates.utcMillis(minimum.toLocalDate()), minimum))
            assertTrue(
                WizardPickerDates.isSelectable(WizardPickerDates.utcMillis(minimum.toLocalDate().plusDays(1)), minimum),
            )
            assertTrue(WizardPickerDates.isSelectable(WizardPickerDates.utcMillis(LocalDate.of(9999, 12, 31)), minimum))
            assertFalse(WizardPickerDates.isSelectableYear(2039, minimum))
            assertTrue(WizardPickerDates.isSelectableYear(2040, minimum))
            assertTrue(WizardPickerDates.isSelectable(0, null))
            assertTrue(WizardPickerDates.isSelectableYear(1, null))
        }

        @Test fun initialValuesUseStoredThenNearestEarlierThenLocalNow() {
            val now = Instant.parse("2026-10-01T09:00:00Z")
            val field = WizardFlightTiming.Field.arrival(0)
            var draft =
                MemberWizardDraft(
                    timingRoute("AMS", "NRT").updateLeg(0) {
                        it.copy(departLocal = "2026-10-12T14:40", arriveLocal = "2026-10-13T09:15")
                    },
                    MemberWizardState(airports = timingAirports),
                )
            assertEquals(LocalDate.of(2026, 10, 13), WizardPickerDates.initialDate(draft, field, now))
            assertEquals(LocalTime.of(9, 15), WizardPickerDates.initialTime(draft, field, now))
            draft = draft.copy(form = draft.form.updateLeg(0) { it.copy(arriveLocal = "") })
            assertEquals(LocalDate.of(2026, 10, 12), WizardPickerDates.initialDate(draft, field, now))
            assertEquals(LocalTime.of(21, 40), WizardPickerDates.initialTime(draft, field, now))
            draft =
                draft.copy(
                    state =
                        draft.state.copy(
                            pendingDateTimes =
                                mapOf(
                                    "${draft.form.legs[0].id}.arriveLocal" to
                                        WizardPendingDateTime("2040-03-25", "07:03"),
                                ),
                        ),
                )
            assertEquals(LocalDate.of(2040, 3, 25), WizardPickerDates.initialDate(draft, field, now))
            assertEquals(LocalTime.of(7, 3), WizardPickerDates.initialTime(draft, field, now))
            draft =
                draft.copy(
                    form =
                        draft.form.updateLeg(0) {
                            it.copy(departLocal = "")
                        },
                    state = draft.state.copy(pendingDateTimes = emptyMap()),
                )
            assertEquals(LocalDate.of(2026, 10, 1), WizardPickerDates.initialDate(draft, field, now))
            assertEquals(LocalTime.of(18, 0), WizardPickerDates.initialTime(draft, field, now))
        }
    }
}
