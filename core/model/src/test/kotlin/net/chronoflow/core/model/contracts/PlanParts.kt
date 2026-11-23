@file:Suppress("ktlint:standard:filename")

package net.chronoflow.core.model.contracts

import kotlinx.serialization.Serializable

@Serializable
data class Parts(
    val plan: PlanView,
    val notices: PlanNoticesView,
    val timeline: TimelinePresentationView,
)
