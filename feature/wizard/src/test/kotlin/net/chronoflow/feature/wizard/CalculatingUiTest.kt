package net.chronoflow.feature.wizard

import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardPresentation
import net.chronoflow.feature.wizard.calculating.CalculatingUi
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatingUiTest {
    @Test fun calculatingUsesTheOutboundDestinationAndSummaryDayCount() {
        val base = fineTuneDraft()
        val draft =
            base.copy(
                form = routeDraft(home = true).form,
                state = base.state.copy(step = MemberWizardStep.calculating, review = generationReview()),
            )
        val ui = CalculatingUi.from(draft)!!
        assertEquals("AMS → NRT", ui.route)
        assertEquals(WizardPresentation.planDayCount(draft.state.review?.summary), ui.days)
    }

    @Test fun missingSummaryHasZeroDays() {
        val base = fineTuneDraft()
        val draft =
            base.copy(
                state =
                    base.state.copy(
                        step = MemberWizardStep.calculating,
                        review = base.state.review!!.copy(summary = null),
                    ),
            )
        assertEquals(0, CalculatingUi.from(draft)!!.days)
    }
}
