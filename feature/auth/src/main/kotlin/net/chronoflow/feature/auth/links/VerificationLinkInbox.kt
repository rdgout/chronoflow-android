package net.chronoflow.feature.auth.links

import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.api.SignedVerificationInput
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.feature.auth.model.VerificationContext

class VerificationLinkInbox {
    var pending: SignedVerificationInput? = null
        private set
    private var presence: SessionPresence? = null
    private var epoch: Long? = null
    private var verified = false

    fun sessionChanged(
        presence: SessionPresence,
        epoch: Long,
        hasVerifiedEmail: Boolean = false,
    ) {
        this.epoch?.let { previous ->
            if (epoch < previous) return
            if (epoch != previous) {
                val firstGuestSignIn =
                    presence is SessionPresence.SignedIn &&
                        this.presence == SessionPresence.SignedOut && epoch == previous + 1
                if (!firstGuestSignIn) pending = null
            }
        }
        this.presence = presence
        this.epoch = epoch
        verified = hasVerifiedEmail
        if (verified) pending = null
        val signedIn = presence as? SessionPresence.SignedIn
        if (signedIn != null && pending?.let { !matches(it.id, signedIn.accountId) } == true) pending = null
    }

    fun receive(input: SignedVerificationInput?): Boolean {
        if (input == null || verified) return false
        val signedIn = presence as? SessionPresence.SignedIn
        if (signedIn != null && !matches(input.id, signedIn.accountId)) return false
        pending = input
        return true
    }

    fun take(context: VerificationContext): SignedVerificationInput? {
        val input = pending ?: return null
        if (verified || context.epoch != epoch || presence != SessionPresence.SignedIn(context.accountId) ||
            !matches(input.id, context.accountId)
        ) {
            return null
        }
        pending = null
        return input
    }

    private fun matches(
        first: String,
        second: String,
    ): Boolean =
        try {
            AccountID.of(first) == AccountID.of(second)
        } catch (_: AccountID.InvalidUUID) {
            false
        }
}
