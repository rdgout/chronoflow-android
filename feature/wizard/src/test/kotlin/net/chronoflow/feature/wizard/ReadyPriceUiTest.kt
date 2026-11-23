package net.chronoflow.feature.wizard

import net.chronoflow.core.model.contracts.ApiReviewPrice
import net.chronoflow.core.model.contracts.ReviewCreditState
import net.chronoflow.core.model.contracts.TripDirection
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.model.WizardCredits
import net.chronoflow.feature.wizard.ready.PriceLine
import net.chronoflow.feature.wizard.ready.PriceValue
import net.chronoflow.feature.wizard.ready.ReadyFooterUi
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ReadyPriceUiTest(
    private val name: String,
    private val price: ApiReviewPrice?,
    private val save: Boolean,
    private val credits: WizardCredits,
    private val value: PriceValue?,
    private val lines: List<PriceLine>,
) {
    @Test fun priceStatesFollowTheServer() {
        val ui =
            readyActive(
                generationReview().copy(
                    price = price,
                    canSave = save,
                    saveBlockedReason = if (save) null else "Not enough credits",
                ),
            ).ready()
        assertEquals(credits, ui.credits)
        assertEquals(value, ui.price?.value)
        assertEquals(if (price == null) null else credits != WizardCredits.unlimited, ui.price?.showLabel)
        assertEquals(if (price == null) null else lines, ui.price?.lines)
        assertEquals(credits == WizardCredits.unavailable, ui.footer == ReadyFooterUi.CloseOnly)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any?>> {
            val base = ApiReviewPrice(ReviewCreditState.Create, 1, 3, false, emptyList())
            return listOf(
                arrayOf(
                    "Ready",
                    base,
                    true,
                    WizardCredits.credits,
                    PriceValue.OneCredit,
                    listOf(PriceLine.Balance(3, false)),
                ),
                arrayOf(
                    "Return",
                    base.copy(cost = 2),
                    true,
                    WizardCredits.credits,
                    PriceValue.Credits(2),
                    listOf(PriceLine.Balance(3, false)),
                ),
                arrayOf(
                    "Unlimited",
                    base.copy(state = ReviewCreditState.Unlimited, cost = 0, unlimited = true),
                    true,
                    WizardCredits.unlimited,
                    PriceValue.Unlimited,
                    emptyList<PriceLine>(),
                ),
                arrayOf(
                    "Free",
                    base.copy(state = ReviewCreditState.Free, cost = 0),
                    true,
                    WizardCredits.free,
                    PriceValue.Free,
                    listOf(PriceLine.FreeTrip),
                ),
                arrayOf(
                    "OneFree",
                    base.copy(freeDirections = listOf(TripDirection.Return)),
                    true,
                    WizardCredits.oneFree,
                    PriceValue.OneCredit,
                    listOf(PriceLine.FreeHome, PriceLine.Balance(3, false)),
                ),
                arrayOf(
                    "NoCredits",
                    base.copy(balance = 0),
                    false,
                    WizardCredits.unavailable,
                    PriceValue.OneCredit,
                    listOf(PriceLine.Balance(0, true)),
                ),
                arrayOf("Null", null, false, WizardCredits.unavailable, null, emptyList<PriceLine>()),
            )
        }
    }
}
