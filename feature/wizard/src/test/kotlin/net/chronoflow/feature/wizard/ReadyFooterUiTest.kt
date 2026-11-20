package net.chronoflow.feature.wizard

import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.model.WizardSaveFailure
import net.chronoflow.feature.wizard.ready.PriceLine
import net.chronoflow.feature.wizard.ready.PriceValue
import net.chronoflow.feature.wizard.ready.ReadyButtonLabel
import net.chronoflow.feature.wizard.ready.ReadyFooterUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadyFooterUiTest {
    @Test fun reviewFixtureRendersOneCreditAndSingularBalance() {
        val price = readyActive().ready().price!!
        assertEquals(PriceValue.OneCredit, price.value)
        assertEquals(listOf(PriceLine.Balance(1, false)), price.lines)
        assertEquals("1", price.value.copy.arguments["count"])
        assertEquals(
            "1",
            price.lines
                .single()
                .copy.arguments["count"],
        )
    }

    @Test fun priceNullHasNoPriceBlockAndCloseOnly() {
        val ui = readyActive(generationReview().copy(price = null)).ready()
        assertNull(ui.price)
        assertEquals(ReadyFooterUi.CloseOnly, ui.footer)
    }

    @Test fun missingReviewHasPrimaryWithoutPrice() {
        val ui = readyActive(null).ready()
        assertNull(ui.price)
        assertNull(ui.summary)
        assertEquals(ReadyFooterUi.Primary(ReadyButtonLabel.MakePlan, false, false), ui.footer)
    }

    @Test fun checkingIsBusyAndDisabled() {
        assertEquals(
            ReadyFooterUi.Primary(ReadyButtonLabel.Checking, false, true),
            readyActive(reviewing = true).ready().footer,
        )
    }

    @Test fun countdownDoesNotSaveAtZero() {
        val active = readyActive(retry = 42)
        val draft = active.wizard.draft
        val failure =
            draft.copy(
                state = draft.state.copy(saveFailure = WizardSaveFailure(WizardSaveFailure.Kind.throttle)),
            )
        val throttled = active.copy(wizard = active.wizard.copy(draft = failure))
        assertEquals(ReadyFooterUi.Primary(ReadyButtonLabel.Countdown(42), false, false), throttled.ready().footer)
        assertEquals(
            ReadyFooterUi.Primary(ReadyButtonLabel.Retry, true, false),
            throttled.copy(wizard = throttled.wizard.copy(retrySeconds = 0)).ready().footer,
        )
    }
}
