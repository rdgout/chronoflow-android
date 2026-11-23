package net.chronoflow.feature.wizard

import net.chronoflow.core.wizard.model.WizardCredits
import net.chronoflow.feature.wizard.ready.PriceLine
import net.chronoflow.feature.wizard.ready.PriceValue
import net.chronoflow.feature.wizard.ready.ReadyUiMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadyEditPriceRulesTest {
    @Test fun editUnlimitedFreeShowsFreeWithoutPriceLabel() {
        val base =
            editReadyActive()
                .wizard.draft.state.review!!
                .price!!
        val ui = ReadyUiMapper.price(base.copy(unlimited = true), WizardCredits.unlimited, editing = true)
        assertEquals(PriceValue.Free, ui.value)
        assertEquals(false, ui.showLabel)
    }

    @Test fun editFreeNeverShowsTheFreeTripLine() {
        val ui = editReadyActive().ready()
        assertEquals(listOf(PriceLine.EditNote), ui.price!!.lines)
    }
}
