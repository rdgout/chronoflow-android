@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.feature.wizard.finetune.AnswerText
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardStepKey
import net.chronoflow.feature.wizard.shell.WizardUiOverride
import net.chronoflow.feature.wizard.ui.WizardCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TripWizardViewModelFineTuneTest {
    @get:Rule val main: org.junit.rules.TestWatcher = WizardMainDispatcherRule()

    @Test fun rowsKeepPromptOrderAndLabels() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = fineTuneDraft() }) { f ->
                runCurrent()
                val ui =
                    f.model.uiState.value!!
                        .fineTune!!
                assertEquals(
                    listOf(
                        PromptKind.Room,
                        PromptKind.LatestWake,
                        PromptKind.SeatSide,
                        PromptKind.LateCheckOut,
                        PromptKind.PreDepartureBusyHours,
                        PromptKind.Cups,
                    ),
                    ui.rows.map { it.kind },
                )
                assertEquals(listOf(true, true, false, false, false, false), ui.rows.map { it.required })
                assertEquals(
                    listOf(
                        AnswerText.Copy(WizardCopy("When I land")),
                        AnswerText.Literal("08:30"),
                        AnswerText.Copy(WizardCopy("Window left")),
                        AnswerText.Copy(WizardCopy("No")),
                        AnswerText.Copy(
                            WizardCopy(
                                "{{count}} time ranges",
                                mapOf("count" to "0"),
                            ),
                        ),
                        AnswerText.Copy(
                            WizardCopy(
                                "{{count}} a day",
                                mapOf("count" to "2"),
                            ),
                        ),
                    ),
                    ui.rows.map { it.answer },
                )
                assertEquals("Dark room at your stay", ui.rows[0].labelKey)
                assertNull(ui.rows[0].suffix)
            }
        }

    @Test fun multiFlightRowsCarryRouteAndCity() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                val base = fineTuneDraft()
                val form = base.form.insertStop(DXB, 0)
                val prompts =
                    listOf(
                        completionPrompt(PromptKind.Room),
                        completionPrompt(PromptKind.Room, 1),
                        completionPrompt(PromptKind.SeatSide),
                        completionPrompt(
                            PromptKind.SeatSide,
                            1,
                        ),
                        completionPrompt(PromptKind.LateCheckOut),
                    )
                draft =
                    base.copy(
                        form = form,
                        state = base.state.copy(review = base.state.review!!.copy(prompts = prompts)),
                    )
            }) { f ->
                runCurrent()
                val rows =
                    f.model.uiState.value!!
                        .fineTune!!
                        .rows
                assertEquals(
                    listOf(
                        " · Dubai",
                        " · Tokyo",
                        " · AMS → DXB",
                        " · DXB → NRT",
                        " · Dubai",
                    ),
                    rows.map { it.suffix },
                )
                assertEquals("Dark room", rows[0].labelKey)
                assertEquals("Late check-out", rows[4].labelKey)
            }
        }

    @Test fun noneStateHasNoRows() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = checkedDraft(MemberWizardStep.fineTune) }) { f ->
                runCurrent()
                assertTrue(
                    f.model.uiState.value!!
                        .fineTune!!
                        .none,
                )
                assertEquals(
                    "Done",
                    f.model.uiState.value!!
                        .chrome.primaryLabel
                        ?.key,
                )
            }
        }

    @Test fun preparationDaysClampAndPersist() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = fineTuneDraft() }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.SetPreparationDays(4))
                runCurrent()
                assertEquals(
                    4,
                    f.model.wizard!!
                        .draft.form.preparation.days,
                )
                val before = f.model.wizard!!.draft
                f.model.onEvent(WizardEvent.SetPreparationDays(5))
                runCurrent()
                assertEquals(before, f.model.wizard!!.draft)
                f.model.onEvent(WizardEvent.SetPreparationDays(-5))
                runCurrent()
                assertEquals(
                    0,
                    f.model.wizard!!
                        .draft.form.preparation.days,
                )
            }
        }

    @Test fun enablingSleepOverridePrefillsFromProfileThenSaved() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    fineTuneDraft().let {
                        it.copy(
                            state =
                                it.state.copy(
                                    profile = JsonObject(mapOf("habitual_bed" to JsonPrimitive("23:30"))),
                                    savedProfile =
                                        JsonObject(
                                            mapOf(
                                                "habitualBed" to JsonPrimitive("22:45"),
                                                "habitualWake" to JsonPrimitive("07:03"),
                                            ),
                                        ),
                                ),
                        )
                    }
            }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.SetSleepOverride(true))
                runCurrent()
                val value =
                    f.model.wizard!!
                        .draft.form.override
                assertTrue(value.enabled)
                assertEquals("23:30", value.habitualBed)
                assertEquals("07:03", value.habitualWake)
                f.model.onEvent(WizardEvent.SetSleepOverride(false))
                runCurrent()
                assertEquals(
                    value.copy(enabled = false),
                    f.model.wizard!!
                        .draft.form.override,
                )
                f.model.onEvent(WizardEvent.SetSleepOverride(true))
                runCurrent()
                assertEquals(
                    value,
                    f.model.wizard!!
                        .draft.form.override,
                )
            }
        }

    @Test fun missingProfilePrefillsFromSavedBed() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    fineTuneDraft().let {
                        it.copy(
                            state =
                                it.state.copy(
                                    profile = JsonObject(emptyMap()),
                                    savedProfile = JsonObject(mapOf("habitualBed" to JsonPrimitive("22:45"))),
                                ),
                        )
                    }
            }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.SetSleepOverride(true))
                runCurrent()
                assertEquals(
                    "22:45",
                    f.model.wizard!!
                        .draft.form.override.habitualBed,
                )
            }
        }

    @Test fun guestHasNoSleepSwitch() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft = fineTuneDraft().let { it.copy(state = it.state.copy(mode = WizardMode.guest)) }
            }) { f ->
                runCurrent()
                assertFalse(
                    f.model.uiState.value!!
                        .fineTune!!
                        .showSleepSwitch,
                )
                f.model.onEvent(WizardEvent.SetSleepOverride(true))
                runCurrent()
                assertFalse(
                    f.model.wizard!!
                        .draft.form.override.enabled,
                )
            }
        }

    @Test fun overrideTimeKeepsMinutePrecision() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = fineTuneDraft() }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.SetSleepOverride(true))
                runCurrent()
                f.model.onEvent(WizardEvent.OpenTimeSheet("override.habitual_wake"))
                runCurrent()
                assertTrue(
                    f.model.uiState.value!!
                        .fineTune!!
                        .openTime!!
                        .minutePrecision,
                )
                f.model.onEvent(WizardEvent.ConfirmTimeSheet("override.habitual_wake", 7, 3, true))
                runCurrent()
                assertEquals(
                    "07:03",
                    f.model.wizard!!
                        .draft.form.override.habitualWake,
                )
                val before = f.model.wizard!!.draft
                f.model.onEvent(WizardEvent.OpenTimeSheet("override.habitual_wake"))
                f.model.onEvent(WizardEvent.ConfirmTimeSheet("override.habitual_wake", 12, 0, false))
                runCurrent()
                assertEquals(before, f.model.wizard!!.draft)
            }
        }

    @Test fun preferenceIgnoresRetiredKeys() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = fineTuneDraft() }) { f ->
                runCurrent()
                val before = f.model.wizard!!.draft
                f.model.onEvent(WizardEvent.SetPreference("pregnancy", true))
                runCurrent()
                assertEquals(before, f.model.wizard!!.draft)
                f.model.onEvent(WizardEvent.SetPreference("exclude_caffeine", true))
                runCurrent()
                assertEquals(
                    true,
                    f.model.uiState.value!!
                        .fineTune!!
                        .preferences["exclude_caffeine"],
                )
            }
        }

    @Test fun unchangedFineTuneGoesStraightToReady() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = checkedDraft(MemberWizardStep.fineTune) }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.Continue(WizardStepKey.FineTune))
                runCurrent()
                assertEquals(
                    MemberWizardStep.ready,
                    f.model.uiState.value!!
                        .step.step,
                )
                assertEquals(0, f.deps.source.count("review"))
            }
        }

    @Test fun changedFineTuneReviewsInPlace() =
        runTest(timeout = 60.seconds) {
            val held = CompletableDeferred<Unit>()
            withWizard(configure = {
                draft = checkedDraft(MemberWizardStep.fineTune)
                source.configure(beforeReview = { held.await() })
            }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.SetPreparationDays(4))
                f.model.onEvent(WizardEvent.Continue(WizardStepKey.FineTune))
                runCurrent()
                val ui = f.model.uiState.value!!
                assertEquals(WizardStepKey.FineTune, ui.step)
                assertTrue(ui.fineTune!!.reviewing)
                assertTrue(ui.chrome.primaryBusy)
                assertFalse(ui.chrome.continueEnabled)
                held.complete(Unit)
                runCurrent()
                assertEquals(
                    MemberWizardStep.ready,
                    f.model.uiState.value!!
                        .step.step,
                )
                assertEquals(1, f.deps.source.count("review"))
            }
        }

    @Test fun fineTuneErrorsAreSortedOverrideAndPreparation() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    fineTuneDraft().copy(
                        fieldErrors =
                            mapOf(
                                "preparation.latestWake" to FieldMessage("error"),
                                "override.habitualBed" to FieldMessage("error"),
                                "legs.0.toIata" to FieldMessage("error"),
                            ),
                    )
            }) { f ->
                runCurrent()
                assertEquals(
                    listOf(
                        "override.habitualBed",
                        "preparation.latestWake",
                    ),
                    f.model.uiState.value!!
                        .fineTune!!
                        .fineTuneErrors
                        .map { it.first },
                )
            }
        }

    @Test fun uiOverrideReviewingShowsBusy() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft = fineTuneDraft()
                uiOverride = WizardUiOverride(reviewing = true)
            }) { f ->
                runCurrent()
                assertTrue(
                    f.model.uiState.value!!
                        .chrome.primaryBusy,
                )
                assertFalse(
                    f.model.uiState.value!!
                        .chrome.continueEnabled,
                )
            }
        }
}
