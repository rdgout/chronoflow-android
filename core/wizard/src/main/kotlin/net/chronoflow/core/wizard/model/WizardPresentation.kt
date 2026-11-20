@file:Suppress("ktlint:standard:enum-entry-name-case")

package net.chronoflow.core.wizard.model

import kotlinx.serialization.Serializable
import net.chronoflow.core.model.contracts.ApiReviewPrice

object WizardPresentation {
    @Serializable
    data class SourceChange(
        val form: WizardForm,
        val fields: List<Field>,
        val price: ApiReviewPrice?,
        val previousPrice: ApiReviewPrice? = null,
    ) {
        enum class Field { route, flight, returnFlight, sleep, extras }
    }
}
