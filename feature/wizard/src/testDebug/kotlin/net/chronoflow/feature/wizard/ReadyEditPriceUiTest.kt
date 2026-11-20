package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.wizard.model.WizardCredits
import net.chronoflow.feature.wizard.ready.PriceLine
import net.chronoflow.feature.wizard.ready.PriceValue
import net.chronoflow.feature.wizard.ready.ReadyButtonLabel
import net.chronoflow.feature.wizard.ready.ReadyFooterUi
import net.chronoflow.feature.wizard.ui.WizardCopy
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class ReadyEditPriceUiTest(
    private val board: String,
    private val credits: WizardCredits,
    private val lines: List<PriceLine>,
    private val enabled: Boolean,
) {
    @Test fun editPriceTitleAndFooterFollowTheOriginalTrip() =
        runTest(timeout = 60.seconds) {
            val ui = checkNotNull(boardState("Wizard" + board, Locale.ENGLISH).ready)
            assertEquals(WizardCopy("Edit your trip"), ui.title)
            assertEquals(credits, ui.credits)
            val price = checkNotNull(ui.price)
            assertEquals(true, price.showLabel)
            assertEquals(if (credits == WizardCredits.free) PriceValue.Free else PriceValue.OneCredit, price.value)
            assertEquals(lines, price.lines)
            assertEquals(
                if (credits == WizardCredits.unavailable) {
                    ReadyFooterUi.CloseOnly
                } else {
                    ReadyFooterUi.Primary(ReadyButtonLabel.Update, enabled, false)
                },
                ui.footer,
            )
            assertEquals(WizardCopy("Update my plan"), ReadyButtonLabel.Update.copy)
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("Edit", WizardCredits.free, listOf(PriceLine.EditNote), false),
                arrayOf(
                    "EditChanged",
                    WizardCredits.credits,
                    listOf(PriceLine.Balance(3, false), PriceLine.EditNote),
                    true,
                ),
                arrayOf("EditNoReturn", WizardCredits.free, listOf(PriceLine.NoRefund, PriceLine.EditNote), true),
                arrayOf(
                    "EditReturn",
                    WizardCredits.credits,
                    listOf(PriceLine.Balance(3, false), PriceLine.EditReturnNote),
                    true,
                ),
                arrayOf(
                    "EditReturnNoCredits",
                    WizardCredits.unavailable,
                    listOf(PriceLine.Balance(0, true), PriceLine.EditReturnNote),
                    true,
                ),
                arrayOf("EditWeb", WizardCredits.free, listOf(PriceLine.EditNote), true),
            )
    }
}
