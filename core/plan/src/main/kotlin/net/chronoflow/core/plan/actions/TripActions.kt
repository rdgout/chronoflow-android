package net.chronoflow.core.plan.actions

import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.plan.PageCopy
import net.chronoflow.core.plan.tripDatesText

enum class TripAction { Edit, Duplicate }

data class TripActionsAvailability(
    val canEdit: Boolean,
    val canDuplicate: Boolean,
    val online: Boolean,
    val busy: Boolean,
) {
    val edit: Boolean get() = canEdit && online && !busy
    val duplicate: Boolean get() = canDuplicate && online && !busy

    fun enabled(action: TripAction): Boolean = if (action == TripAction.Edit) edit else duplicate
}

data class TripActionsHeader(
    val title: String,
    val subtitle: String,
)

fun tripActionsHeader(
    card: TripCardView,
    detail: TripDetail?,
    copy: PageCopy,
): TripActionsHeader {
    val title = copy("%@ → %@", card.from.city ?: card.from.iata, card.to.city ?: card.to.iata)
    val parts =
        mutableListOf(
            tripDatesText(
                card.departLocal,
                detail
                    ?.plan
                    ?.days
                    ?.lastOrNull()
                    ?.labelDate,
                copy.language,
                pageCopy = copy,
            ),
        )
    if (card.connections.isNotEmpty()) {
        parts.add(
            copy("via %@", card.connections.joinToString(", ") { it.city ?: it.iata }),
        )
    }
    if (detail?.trip?.legs?.any { it.returnLeg } == true) parts.add(copy("with return"))
    return TripActionsHeader(title, parts.joinToString(" · "))
}
