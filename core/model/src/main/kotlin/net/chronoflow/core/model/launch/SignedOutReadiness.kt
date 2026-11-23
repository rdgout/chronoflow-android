package net.chronoflow.core.model.launch

object SignedOutReadiness : LaunchReadiness {
    override suspend fun session(): SessionPresence = SessionPresence.SignedOut

    override suspend fun gate(): Gate = Gate.App

    override suspend fun hasStoredData(): Boolean = true

    override suspend fun allowFirstLoad() = Unit
}
