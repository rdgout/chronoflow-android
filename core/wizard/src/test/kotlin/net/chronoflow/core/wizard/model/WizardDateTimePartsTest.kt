@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.wizard.model

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.chronoflow.core.model.FieldMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

class WizardDateTimePartsTest {
    private val date = LocalDate.of(2026, 10, 12)

    private suspend fun TestScope.withParts(block: suspend (MemberWizardModel) -> Unit) {
        val fixture = WizardModelFixture(backgroundScope, testScheduler)
        val model = fixture.model
        model.form = model.form.updateLeg(0) { it.copy(departLocal = "", arriveLocal = "") }
        try {
            block(model)
        } finally {
            fixture.close()
        }
    }

    @Test fun datePartAloneStaysPendingAndTheLegStaysEmpty() =
        runTest(timeout = 60.seconds) {
            withParts { model ->
                model.setDatePart(0, false, date)
                assertEquals("", model.form.legs[0].departLocal)
                assertEquals("2026-10-12", model.datePart(0, false))
                assertNull(model.timePart(0, false))
                assertTrue(model.state.entered)
                assertFalse(model.outboundComplete)
            }
        }

    @Test fun secondHalfComposesTheWallValueAndClearsPending() =
        runTest(timeout = 60.seconds) {
            withParts {
                it.setDatePart(0, false, date)
                it.setTimePart(0, false, 14, 40)
                assertEquals("2026-10-12T14:40", it.form.legs[0].departLocal)
                assertTrue(it.state.pendingDateTimes.isEmpty())
            }
        }

    @Test fun changingOneHalfKeepsTheOther() =
        runTest(timeout = 60.seconds) {
            withParts {
                it.setDateTime("2026-10-12T14:40", 0, false)
                it.setDatePart(0, false, date.plusDays(1))
                assertEquals("2026-10-13T14:40", it.form.legs[0].departLocal)
                it.setTimePart(0, false, 9, 5)
                assertEquals("2026-10-13T09:05", it.form.legs[0].departLocal)
            }
        }

    @Test fun minutesAndHoursArePadded() =
        runTest(timeout = 60.seconds) {
            withParts {
                it.setTimePart(0, false, 7, 3)
                assertEquals("07:03", it.timePart(0, false))
                it.setTimePart(0, false, 0, 0)
                assertEquals("00:00", it.timePart(0, false))
                it.setDatePart(0, false, date)
                assertEquals("2026-10-12T00:00", it.form.legs[0].departLocal)
            }
        }

    @Test fun anyWriteClearsFieldErrorsIncludingTheBanner() =
        runTest(timeout = 60.seconds) {
            withParts {
                it.draft =
                    it.draft.copy(
                        fieldErrors =
                            mapOf(
                                "legs.0.arriveLocal" to FieldMessage("error"),
                                "review" to FieldMessage("banner"),
                            ),
                    )
                it.setTimePart(0, false, 7, 3)
                assertTrue(it.draft.fieldErrors.isEmpty())
            }
        }

    private suspend fun TestScope.routeEdit(operation: (MemberWizardModel) -> Unit) =
        withParts {
            it.form =
                it.form.insertStop(
                    net.chronoflow.core.model.contracts
                        .ApiAirportSuggestion("DXB", "Dubai", "Dubai", "AE", "Asia/Dubai"),
                    0,
                )
            it.setDatePart(0, false, date)
            assertFalse(it.state.pendingDateTimes.isEmpty())
            operation(it)
            assertTrue(it.state.pendingDateTimes.isEmpty())
        }

    @Test fun routeEditDropsPendingHalves_chooseAirport() =
        runTest(timeout = 60.seconds) {
            routeEdit {
                it.chooseAirport(
                    net.chronoflow.core.model.contracts.ApiAirportSuggestion(
                        "LHR",
                        "Heathrow",
                        "London",
                        "GB",
                        "Europe/London",
                    ),
                    WizardAirportSheet(0, WizardAirportSheet.Side.from),
                )
            }
        }

    @Test fun routeEditDropsPendingHalves_swapAirports() =
        runTest(timeout = 60.seconds) {
            withParts {
                it.setDatePart(0, false, date)
                it.swapAirports()
                assertTrue(it.state.pendingDateTimes.isEmpty())
            }
        }

    @Test fun routeEditDropsPendingHalves_removeStop() =
        runTest(
            timeout = 60.seconds,
        ) { routeEdit { it.removeStop(1) } }

    @Test fun routeEditDropsPendingHalves_makeDirect() = runTest(timeout = 60.seconds) { routeEdit { it.makeDirect() } }

    @Test fun routeEditDropsPendingHalves_chooseReturn() =
        runTest(timeout = 60.seconds) {
            routeEdit {
                it.go(MemberWizardStep.returnChoice)
                it.chooseReturn("return_legs")
            }
        }

    @Test fun pendingHalvesSurviveRecoveryEncoding() =
        runTest(timeout = 60.seconds) {
            withParts {
                it.setDatePart(0, false, date)
                val encoded = Json.encodeToString(it.draft)
                assertEquals(it.draft, Json.decodeFromString<MemberWizardDraft>(encoded))
                assertTrue(Json.decodeFromString<MemberWizardState>("{}").pendingDateTimes.isEmpty())
            }
        }

    @Test fun pendingHalvesAreNotInTheReviewInputOrFingerprint() =
        runTest(timeout = 60.seconds) {
            withParts {
                val fingerprint = it.inputFingerprint
                val input = it.reviewInput
                it.setDatePart(0, false, date)
                assertEquals(fingerprint, it.inputFingerprint)
                assertEquals(input, it.reviewInput)
            }
        }

    @Test fun closedModelIgnoresPartWrites() =
        runTest(timeout = 60.seconds) {
            withParts {
                it.discard()
                val before = it.draft
                it.setDatePart(0, false, date)
                it.setTimePart(0, false, 7, 3)
                assertEquals(before, it.draft)
            }
        }

    @Test fun invalidHourOrMinuteIsIgnored() =
        runTest(timeout = 60.seconds) {
            withParts {
                val before = it.draft
                it.setTimePart(0, false, 24, 0)
                it.setTimePart(0, false, 0, 60)
                it.setTimePart(-1, false, 7, 3)
                it.setDatePart(100, false, date)
                assertEquals(before, it.draft)
            }
        }
}
