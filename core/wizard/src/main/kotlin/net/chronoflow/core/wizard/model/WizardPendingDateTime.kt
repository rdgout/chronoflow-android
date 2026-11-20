package net.chronoflow.core.wizard.model

import kotlinx.serialization.Serializable

@Serializable
data class WizardPendingDateTime(
    val date: String? = null,
    val time: String? = null,
)
