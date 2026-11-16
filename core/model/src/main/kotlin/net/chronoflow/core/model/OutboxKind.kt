package net.chronoflow.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class OutboxKind {
    @SerialName("check-off")
    CheckOff,

    @SerialName("settings")
    Settings,

    @SerialName("complete-trip")
    CompleteTrip,

    @SerialName("profile")
    Profile,

    @SerialName("data-export")
    DataExport,

    @SerialName("security")
    Security,

    @SerialName("walkthrough")
    Walkthrough,
}
