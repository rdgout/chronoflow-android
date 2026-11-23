@file:Suppress("ktlint:standard:enum-entry-name-case")

package net.chronoflow.core.wizard.model

import java.time.Instant

object GuestRecoveryGate {
    enum class Decision { none, wait, expire, wizard, signIn }

    fun decision(
        updatedAt: Instant?,
        now: Instant,
        handoffFinished: Boolean,
        introFinished: Boolean,
        signingIn: Boolean,
    ): Decision =
        when {
            updatedAt == null -> Decision.none
            MemberWizardRecord.isExpired(updatedAt.toEpochMilli(), now.toEpochMilli()) -> Decision.expire
            !handoffFinished || !introFinished -> Decision.wait
            signingIn -> Decision.signIn
            else -> Decision.wizard
        }
}
