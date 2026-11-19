package net.chronoflow.core.model.session

sealed interface SessionPresence {
    data object SignedOut : SessionPresence

    data class SignedIn(
        val accountId: String,
    ) : SessionPresence
}
