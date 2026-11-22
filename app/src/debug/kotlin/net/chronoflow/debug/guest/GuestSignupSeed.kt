package net.chronoflow.debug.guest

import net.chronoflow.app.guest.GuestSignupSeeding
import net.chronoflow.feature.auth.model.GuestSignupState

class GuestSignupSeed(
    override val state: GuestSignupState,
) : GuestSignupSeeding {
    companion object {
        fun board(name: String): GuestSignupSeed? =
            when (name) {
                "GuestSignup" -> GuestSignupSeed(GuestSignupState.Ready)
                "GuestSignupFailed" -> GuestSignupSeed(GuestSignupState.Failed)
                "GuestSignupRetry" -> GuestSignupSeed(GuestSignupState.Retry)
                "GuestSignupReview" -> GuestSignupSeed(GuestSignupState.Review)
                else -> null
            }
    }
}
