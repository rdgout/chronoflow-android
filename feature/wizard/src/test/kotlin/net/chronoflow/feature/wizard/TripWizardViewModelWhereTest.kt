@file:Suppress("ktlint:standard:max-line-length")
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.wizard.model.MemberWizardDestination
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardAirportSheet
import net.chronoflow.core.wizard.model.hasChanges
import net.chronoflow.core.wizard.model.makeDirect
import net.chronoflow.feature.wizard.shell.WizardStepKey
import net.chronoflow.feature.wizard.ui.StopQuestionUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TripWizardViewModelWhereTest {
    @get:Rule val main: org.junit.rules.TestWatcher = WizardMainDispatcherRule()

    @Test fun choosingFromAndToCompletesTheRouteAndMarksEntered() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { }) { f ->
                runCurrent()
                assertFalse(f.model.wizard!!.hasChanges)
                f.model.openAirport(WizardAirportSheet(0, WizardAirportSheet.Side.from))
                f.model.chooseAirport(
                    AMS,
                    f.model.wizard!!
                        .draft.state.sheet!!,
                )
                f.model.openAirport(WizardAirportSheet(0, WizardAirportSheet.Side.to))
                f.model.chooseAirport(
                    NRT,
                    f.model.wizard!!
                        .draft.state.sheet!!,
                )
                runCurrent()
                f.model.wizard!!.flush()
                assertTrue(f.model.wizard!!.hasChanges)
                assertTrue(
                    f.model.uiState.value!!
                        .chrome.continueEnabled,
                )
                assertTrue(
                    f.deps.recovery.last!!
                        .state.revision > 0,
                )
            }
        }

    @Test fun swapExchangesTheEndsAndClearsTheLastTripHint() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    routeDraft()
            }) { f ->
                runCurrent()
                f.model.swapAirports()
                runCurrent()
                assertEquals(
                    "NRT",
                    f.model.where.value!!
                        .rows
                        .first()
                        .iata,
                )
                assertEquals(
                    "AMS",
                    f.model.where.value!!
                        .rows
                        .last()
                        .iata,
                )
                assertNull(
                    f.model.where.value!!
                        .rows
                        .first()
                        .hint,
                )
            }
        }

    @Test fun yesOpensAStopSheetBetweenTheEnds() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    routeDraft()
            }) { f ->
                runCurrent()
                f.model.openAirport(
                    f.model.where.value!!
                        .question!!
                        .addStopSheet,
                )
                runCurrent()
                assertEquals(
                    WizardAirportSheet(0, WizardAirportSheet.Side.stop),
                    f.model.sheet.value!!
                        .sheet,
                )
                assertEquals(
                    StopQuestionUi.Selection.WithStops,
                    f.model.where.value!!
                        .question!!
                        .selected,
                )
            }
        }

    @Test fun choosingAStopInsertsIt() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = routeDraft() }) { f ->
                runCurrent()
                val sheet = WizardAirportSheet(0, WizardAirportSheet.Side.stop)
                f.model.openAirport(sheet)
                f.model.chooseAirport(DXB, sheet)
                runCurrent()
                assertEquals(
                    listOf("AMS", "DXB", "NRT"),
                    f.model.where.value!!
                        .rows
                        .map { it.iata },
                )
            }
        }

    @Test fun replacingAStopKeepsTheLength() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    routeDraft(1)
            }) { f ->
                runCurrent()
                val sheet = WizardAirportSheet(0, WizardAirportSheet.Side.stop, true)
                f.model.openAirport(sheet)
                f.model.chooseAirport(BKK, sheet)
                runCurrent()
                assertEquals(
                    3,
                    f.model.where.value!!
                        .rows.size,
                )
                assertEquals(
                    "BKK",
                    f.model.where.value!!
                        .rows[1]
                        .iata,
                )
            }
        }

    @Test fun removingTheOnlyStopRestoresDirect() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    routeDraft(1)
            }) { f ->
                runCurrent()
                f.model.removeStop(1)
                runCurrent()
                assertEquals(
                    2,
                    f.model.where.value!!
                        .rows.size,
                )
                assertEquals(
                    StopQuestionUi.Selection.Direct,
                    f.model.where.value!!
                        .question!!
                        .selected,
                )
            }
        }

    @Test fun noDirectRemovesEveryStop() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = routeDraft(2) }) { f ->
                runCurrent()
                f.model.makeDirect(false)
                runCurrent()
                assertEquals(
                    2,
                    f.model.where.value!!
                        .rows.size,
                )
            }
        }

    @Test fun homeToFollowsTheOutboundFromUntilChanged() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    routeDraft(home = true)
            }) { f ->
                runCurrent()
                val from = WizardAirportSheet(0, WizardAirportSheet.Side.from)
                f.model.openAirport(from)
                f.model.chooseAirport(DXB, from)
                assertEquals(
                    "DXB",
                    f.model.wizard!!
                        .draft.form.legs
                        .last()
                        .toIata,
                )
                val home = WizardAirportSheet(1, WizardAirportSheet.Side.home)
                f.model.openAirport(home)
                f.model.chooseAirport(BKK, home)
                f.model.openAirport(from)
                f.model.chooseAirport(AMS, from)
                assertEquals(
                    "BKK",
                    f.model.wizard!!
                        .draft.form.legs
                        .last()
                        .toIata,
                )
            }
        }

    @Test fun openSheetIsRestoredFromRecovery() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    routeDraft(
                        1,
                    ).let {
                        it.copy(
                            state = it.state.copy(sheet = WizardAirportSheet(1, WizardAirportSheet.Side.stop)),
                        )
                    }
            }) { f ->
                runCurrent()
                assertEquals(
                    WizardAirportSheet(1, WizardAirportSheet.Side.stop),
                    f.model.uiState.value!!
                        .sheet!!
                        .sheet,
                )
            }
        }

    @Test fun continueFromWhereRunsOnce() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = routeDraft() }) { f ->
                runCurrent()
                f.model.continueFrom(WizardStepKey.Where)
                val revision =
                    f.model.wizard!!
                        .draft.state.revision
                f.model.continueFrom(WizardStepKey.Where)
                runCurrent()
                assertEquals(
                    revision,
                    f.model.wizard!!
                        .draft.state.revision,
                )
                assertEquals(
                    listOf(MemberWizardDestination.outbound),
                    (f.model.state.value as TripWizardUiState.Active).path,
                )
            }
        }

    @Test fun backFromTheHomeRoutePopsToComingBack() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    routeDraft(home = true)
            }) { f ->
                runCurrent()
                f.model.back()
                runCurrent()
                assertEquals(
                    MemberWizardStep.returnChoice,
                    f.model.wizard!!
                        .draft.state.step,
                )
            }
        }

    @Test fun cancelAirportWritesNothingButTheClosedSheet() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    routeDraft()
            }) { f ->
                runCurrent()
                f.model.openAirport(WizardAirportSheet(0, WizardAirportSheet.Side.from))
                val before = f.model.wizard!!.draft
                f.model.cancelAirport()
                val after = f.model.wizard!!.draft
                assertEquals(before.form, after.form)
                assertEquals(before.state.copy(sheet = null, revision = after.state.revision), after.state)
                runCurrent()
                assertNull(f.model.sheet.value)
            }
        }
}
