package net.chronoflow.feature.auth.model

import net.chronoflow.core.model.api.SessionAccount
import net.chronoflow.core.navigation.Route

class AuthNavigation {
    var challengeID: String? = null
        private set

    var forgotEmail: String = ""
        private set

    fun forgot(email: String = "") {
        forgotEmail = email
    }

    fun twoFactor(challengeID: String): Boolean {
        if (challengeID.isBlank()) {
            cancelTwoFactor()
            return false
        }
        this.challengeID = challengeID
        return true
    }

    fun discardChallenge() {
        challengeID = null
    }

    fun cancelTwoFactor() {
        challengeID = null
    }

    fun reset() {
        forgotEmail = ""
        challengeID = null
    }

    override fun toString(): String = "AuthNavigation(challengeID=<redacted>)"

    companion object {
        fun route(account: SessionAccount): Route {
            if (!account.hasVerifiedEmail) return Route.Verify
            if (account.policies.acceptedTermsVersion != account.policies.termsVersion) return Route.Terms
            if (!account.walkthroughSeen) return Route.Walkthrough(replay = false)
            return Route.Today
        }
    }
}
