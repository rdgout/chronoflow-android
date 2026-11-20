@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.wizard.generation.TripEditContext
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.feature.wizard.excluded.ExcludedNote
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardStepKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

internal fun excludedDraft(
    saved: Boolean = false,
    guest: Boolean = false,
): MemberWizardDraft {
    val base = freeDayDraft()
    return base.copy(
        state =
            base.state.copy(
                step = MemberWizardStep.excluded,
                promptSheetID = null,
                sleepNeeded = false,
                sleepCompleted = true,
                mode = if (guest) WizardMode.guest else WizardMode.member,
                profile =
                    JsonObject(
                        base.state.profile + if (saved) emptyMap() else mapOf("age_band" to JsonPrimitive("under_18")),
                    ),
                savedProfile = if (saved) JsonObject(mapOf("shiftWork" to JsonPrimitive(true))) else null,
                review =
                    base.state.review!!.copy(
                        status = PlanStatus.NoPlan,
                        prompts = emptyList(),
                        reasons =
                            listOf(
                                CopyLine("This plan is not available for travellers under 18.", emptyList()),
                            ),
                    ),
            ),
    )
}

class TripWizardViewModelExcludedTest {
    @get:Rule val main: org.junit.rules.TestWatcher = WizardMainDispatcherRule()

    @Test fun noteIsDefaultForASessionAnswer() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = excludedDraft() }) { fixture ->
                runCurrent()
                assertEquals(
                    ExcludedNote.Default,
                    fixture.model.uiState.value!!
                        .excluded!!
                        .note,
                )
            }
        }

    @Test fun noteIsSavedWhenTheAnswerCameFromTheAccount() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = excludedDraft(saved = true) }) { fixture ->
                runCurrent()
                assertEquals(
                    ExcludedNote.Saved,
                    fixture.model.uiState.value!!
                        .excluded!!
                        .note,
                )
            }
        }

    @Test fun noteIsEditWhileEditing() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                val base = excludedDraft(saved = true)
                draft = base.copy(state = base.state.copy(edit = TripEditContext("trip", "source")))
            }) { fixture ->
                runCurrent()
                assertEquals(
                    ExcludedNote.Edit,
                    fixture.model.uiState.value!!
                        .excluded!!
                        .note,
                )
            }
        }

    @Test fun changeMyAnswersClearsOnlyTheExcludingAnswersAndRechecks() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                val base = excludedDraft(saved = true)
                draft =
                    base.copy(
                        state =
                            base.state.copy(
                                profile = JsonObject(base.state.profile + ("age_band" to JsonPrimitive("18_64"))),
                            ),
                    )
            }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.Continue(WizardStepKey.Excluded))
                runCurrent()
                val state =
                    fixture.model.wizard!!
                        .draft.state
                assertEquals(JsonNull, state.profile["shift_work"])
                assertEquals(JsonPrimitive("18_64"), state.profile["age_band"])
                assertEquals(MemberWizardStep.checking, state.step)
                assertEquals(true, state.checkingAnswers)
            }
        }

    @Test fun guestChangeMyAnswersRestartsAtTheFirstQuestion() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = excludedDraft(guest = true) }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.Continue(WizardStepKey.Excluded))
                runCurrent()
                val state =
                    fixture.model.wizard!!
                        .draft.state
                assertEquals(MemberWizardStep.question, state.step)
                assertEquals(0, state.questionIndex)
                assertEquals(JsonNull, state.profile["age_band"])
                assertEquals(JsonNull, state.profile["shift_work"])
                assertTrue(
                    fixture.deps.source.guestInputs
                        .isEmpty(),
                )
                assertTrue(
                    fixture.deps.source.reviewInputs
                        .isEmpty(),
                )
            }
        }

    @Test fun closeNowSkipsTheDiscardAlert() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = excludedDraft() }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.CloseNow)
                assertTrue(fixture.model.closing.value)
                assertFalse(fixture.model.discardDialogVisible.value)
            }
        }

    @Test fun headerCloseOnGuestExcludedClosesDirectly() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = excludedDraft(guest = true) }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.RequestClose)
                assertTrue(fixture.model.closing.value)
                assertFalse(fixture.model.discardDialogVisible.value)
            }
        }

    @Test fun headerCloseOnMemberExcludedWithChangesAsksFirst() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = excludedDraft() }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.RequestClose)
                assertFalse(fixture.model.closing.value)
                assertTrue(fixture.model.discardDialogVisible.value)
            }
        }

    @Test fun footerSlotsAreSetOnlyOnExcluded() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = excludedDraft() }) { fixture ->
                runCurrent()
                val chrome =
                    fixture.model.uiState.value!!
                        .chrome
                assertEquals("excluded", chrome.footerId)
                assertTrue(chrome.secondaryClose)
                assertTrue(chrome.primaryPlain)
                fixture.model.onEvent(WizardEvent.Continue(WizardStepKey.Excluded))
                runCurrent()
                val checking =
                    fixture.model.uiState.value!!
                        .chrome
                assertFalse(checking.secondaryClose)
                assertFalse(checking.primaryPlain)
            }
        }
}
