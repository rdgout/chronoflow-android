@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.feature.wizard.ready.PriceLine
import net.chronoflow.feature.wizard.ready.ReadyFooterUi
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardStepKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ReadyCreditsSaveFailureTest {
    @get:Rule val main: org.junit.rules.TestWatcher = WizardMainDispatcherRule()

    @Test fun creditsFailureRechecksAndShowsCloseOnly() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = checkedDraft(MemberWizardStep.fineTune) }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.Continue(WizardStepKey.FineTune))
                runCurrent()
                val review =
                    f.model.wizard!!
                        .draft.state.review!!
                f.deps.source.queueReviews(
                    listOf(
                        Result.success(
                            review.copy(
                                canSave = false,
                                saveBlockedReason = "Not enough credits",
                                price = review.price!!.copy(balance = 0),
                            ),
                        ),
                    ),
                )
                f.deps.source.configure(saveFailure = DataFailure.NotEnoughCredits)
                f.model.onEvent(WizardEvent.Continue(WizardStepKey.Ready))
                runCurrent()
                val ui =
                    f.model.uiState.value!!
                        .ready!!
                assertEquals(ReadyFooterUi.CloseOnly, ui.footer)
                assertEquals(listOf(PriceLine.Balance(0, true)), ui.price!!.lines)
                assertEquals(1, f.deps.source.count("save"))
                assertEquals(1, f.deps.source.count("review"))
                assertEquals(
                    null,
                    f.deps.source.reviewInputs
                        .single()
                        .identity,
                )
            }
        }
}
