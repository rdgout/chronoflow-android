@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.reviewInput
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardStepKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

internal fun sleepDraft(
    bed: String = "23:30",
    wake: String = "07:15",
): MemberWizardDraft =
    flightDraft().let {
        it.copy(
            state =
                it.state.copy(
                    step = MemberWizardStep.sleep,
                    sleepNeeded = true,
                    lastInputStep = MemberWizardStep.returnChoice,
                    profile =
                        JsonObject(
                            mapOf(
                                "habitual_bed" to JsonPrimitive(bed),
                                "habitual_wake" to JsonPrimitive(wake),
                                "chronotype" to JsonPrimitive("intermediate"),
                            ),
                        ),
                ),
        )
    }

class TripWizardViewModelSleepTest {
    @get:Rule val main: org.junit.rules.TestWatcher = WizardMainDispatcherRule()

    @Test fun confirmTimeSheetStoresTheRoundedValue() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = sleepDraft() }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.OpenTimeSheet("habitual_bed"))
                fixture.model.onEvent(WizardEvent.ConfirmTimeSheet("habitual_bed", 23, 13, true))
                runCurrent()
                assertEquals(
                    JsonPrimitive("23:15"),
                    fixture.model.wizard!!
                        .draft.state.profile["habitual_bed"],
                )
                assertNull(
                    fixture.model.uiState.value!!
                        .sleep!!
                        .openTime,
                )
                assertEquals(
                    JsonPrimitive("23:15"),
                    fixture.deps.recovery.last!!
                        .state.profile["habitual_bed"],
                )
            }
        }

    private fun noWrite(
        stored: String,
        event: WizardEvent,
    ) = runTest(timeout = 60.seconds) {
        withWizard(configure = { draft = sleepDraft(stored) }) { fixture ->
            runCurrent()
            val before = fixture.model.wizard!!.draft
            val writes = fixture.deps.recovery.persisted.size
            fixture.model.onEvent(WizardEvent.OpenTimeSheet("habitual_bed"))
            fixture.model.onEvent(event)
            runCurrent()
            assertEquals(before, fixture.model.wizard!!.draft)
            assertEquals(writes, fixture.deps.recovery.persisted.size)
            assertNull(
                fixture.model.uiState.value!!
                    .sleep!!
                    .openTime,
            )
        }
    }

    @Test fun untouchedConfirmWritesNothing() =
        noWrite(
            "07:12",
            WizardEvent.ConfirmTimeSheet("habitual_bed", 7, 12, false),
        )

    @Test fun confirmRoundingToTheStoredValueWritesNothing() =
        noWrite("07:15", WizardEvent.ConfirmTimeSheet("habitual_bed", 7, 13, true))

    @Test fun dismissWritesNothing() = noWrite("07:12", WizardEvent.DismissTimeSheet)

    @Test fun backClosesTheOpenSheetFirst() = noWrite("07:12", WizardEvent.Back)

    @Test fun notSureKeepsTheChronotypeKeyAsNull() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = sleepDraft() }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.SetSleep("chronotype", JsonNull))
                runCurrent()
                val state =
                    fixture.model.wizard!!
                        .draft.state
                assertTrue("chronotype" in state.profile)
                assertEquals(JsonNull, state.profile["chronotype"])
                assertEquals(
                    "unsure",
                    fixture.model.uiState.value!!
                        .sleep!!
                        .selectedChronotype,
                )
                assertTrue(
                    fixture.model.uiState.value!!
                        .chrome.continueEnabled,
                )
                assertEquals(
                    JsonNull,
                    fixture.model.wizard!!
                        .reviewInput.profile
                        ?.get("chronotype"),
                )
            }
        }

    private fun invalidNight(
        bed: String,
        wake: String,
    ) = runTest(timeout = 60.seconds) {
        withWizard(configure = { draft = sleepDraft(bed, wake) }) { fixture ->
            runCurrent()
            fixture.model.onEvent(WizardEvent.Continue(WizardStepKey.Sleep))
            runCurrent()
            assertEquals(
                MemberWizardStep.sleep,
                fixture.model.wizard!!
                    .draft.state.step,
            )
            assertEquals(
                setOf("profile.habitual_bed"),
                fixture.model.wizard!!
                    .draft.fieldErrors.keys,
            )
            assertEquals(
                1,
                fixture.model.uiState.value!!
                    .sleep!!
                    .continueFailures,
            )
        }
    }

    @Test fun shortNightStaysWithAnError() = invalidNight("23:00", "01:00")

    @Test fun longNightStaysWithAnError() = invalidNight("07:00", "23:00")

    @Test fun anySleepWriteClearsErrors() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft = sleepDraft().copy(fieldErrors = mapOf("legs.0.toIata" to FieldMessage("error")))
            }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.SetSleep("chronotype", JsonPrimitive("early")))
                runCurrent()
                assertTrue(
                    fixture.model.wizard!!
                        .draft.fieldErrors
                        .isEmpty(),
                )
            }
        }
}
