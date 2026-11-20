@file:Suppress("ktlint:standard:filename", "ktlint:standard:enum-entry-name-case")

package net.chronoflow.core.wizard.model

import kotlinx.serialization.Serializable

@Serializable
enum class MemberWizardStep {
    whereTo,
    outbound,
    returnChoice,
    returnRoute,
    returnFlight,
    sleep,
    checking,
    question,
    excluded,
    continuation,
    fineTune,
    ready,
    calculating,
}
