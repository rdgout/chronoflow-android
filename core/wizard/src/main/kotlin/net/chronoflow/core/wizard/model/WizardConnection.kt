package net.chronoflow.core.wizard.model

sealed interface WizardConnection {
    data class Stop(
        val airport: String,
    ) : WizardConnection

    data class Connection(
        val airport: String,
        val minutes: Int,
    ) : WizardConnection

    data class Overnight(
        val airport: String,
        val nights: Int,
    ) : WizardConnection
}
