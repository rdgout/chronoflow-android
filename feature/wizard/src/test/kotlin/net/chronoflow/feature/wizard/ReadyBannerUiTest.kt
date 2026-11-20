package net.chronoflow.feature.wizard

import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.model.WizardSaveFailure
import net.chronoflow.feature.wizard.ready.ReadyBannerUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadyBannerUiTest {
    @Test fun saveFailureBeatsAdjustedAndUpdated() {
        val base = readyActive(generationReview().copy(status = PlanStatus.Modified), retry = 42)
        val draft = base.wizard.draft
        val updated = base.copy(wizard = base.wizard.copy(draft = draft.copy(state = draft.state.copy(updated = true))))
        assertTrue(updated.ready().banner is ReadyBannerUi.Adjusted)
        val failed =
            updated.copy(
                wizard =
                    updated.wizard.copy(
                        draft =
                            draft.copy(
                                state =
                                    draft.state.copy(
                                        updated = true,
                                        saveFailure = WizardSaveFailure(WizardSaveFailure.Kind.throttle),
                                    ),
                            ),
                    ),
            )
        val banner = failed.ready().banner as ReadyBannerUi.SaveFailed
        assertEquals(ReadyBannerUi.SaveFailed(WizardSaveFailure.Kind.throttle, 42), banner)
        assertEquals("42", banner.body.arguments["count"])
        assertEquals("Too many attempts. Please try again later.", banner.copy(retrySeconds = 0).body.key)
    }

    @Test fun adjustedRetainsTheServerReasonsInOrder() {
        val reasons = listOf(CopyLine("reason.one", emptyList()), CopyLine("reason.two", emptyList()))
        assertEquals(
            ReadyBannerUi.Adjusted(reasons),
            readyActive(generationReview().copy(status = PlanStatus.Modified, reasons = reasons)).ready().banner,
        )
    }

    @Test fun updatedDropsMissingSummaryAndPrice() {
        val active = readyActive(generationReview().copy(summary = null, price = null))
        val draft = active.wizard.draft
        assertEquals(
            ReadyBannerUi.Updated(null, null),
            active
                .copy(
                    wizard = active.wizard.copy(draft = draft.copy(state = draft.state.copy(updated = true))),
                ).ready()
                .banner,
        )
    }
}
