package net.chronoflow.core.wizard.model

import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.testing.wizard.generationReview
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class WizardCreditsTests {
    @RunWith(Parameterized::class)
    class MissingPrice(
        private val status: PlanStatus,
    ) {
        @Test fun noPriceIsNeverFree() {
            assertEquals(
                WizardCredits.unavailable,
                WizardCredits.of(generationReview().copy(status = status, price = null, canSave = false)),
            )
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "status={0}")
            fun cases() = listOf(PlanStatus.NeedsAnswers, PlanStatus.NoPlan)
        }
    }

    class Priced {
        @Test fun pricedReviews() {
            val paid = generationReview()
            assertEquals(WizardCredits.credits, WizardCredits.of(paid))
            assertEquals(
                WizardCredits.unavailable,
                WizardCredits.of(paid.copy(canSave = false, saveBlockedReason = "Not enough credits")),
            )
            assertEquals(WizardCredits.free, WizardCredits.of(paid.copy(price = paid.price!!.copy(cost = 0))))
        }
    }
}
