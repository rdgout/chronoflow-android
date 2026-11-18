package net.chronoflow.core.data.session

interface SessionHooks {
    suspend fun onForeground()

    suspend fun onSessionEnding()
}

object NoopSessionHooks : SessionHooks {
    override suspend fun onForeground() = Unit

    override suspend fun onSessionEnding() = Unit
}
