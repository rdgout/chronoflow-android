@file:Suppress("ktlint:standard:enum-entry-name-case")

package net.chronoflow.core.wizard.model

import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.TripDirection

enum class WizardCredits {
    unlimited,
    free,
    oneFree,
    credits,
    unavailable,
    ;

    companion object {
        fun of(review: ApiTripReview): WizardCredits {
            val price = review.price ?: return unavailable
            return when {
                !review.canSave && review.saveBlockedReason != null -> unavailable
                price.unlimited -> unlimited
                price.cost == 0 -> free
                TripDirection.Return in price.freeDirections -> oneFree
                else -> credits
            }
        }
    }
}
