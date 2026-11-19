package net.chronoflow.core.model.session

import kotlinx.coroutines.CancellationException

interface SessionStore {
    suspend fun read(): ByteArray?

    suspend fun write(data: ByteArray)

    suspend fun delete()
}

suspend fun SessionStore.readStoredSession(): StoredSession? {
    val data = read() ?: return null
    return StoredSession.parse(data) ?: run {
        delete()
        null
    }
}

suspend fun SessionStore.loadSession(): SessionPresence =
    try {
        readStoredSession()?.let { SessionPresence.SignedIn(it.accountId) } ?: SessionPresence.SignedOut
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        SessionPresence.SignedOut
    }
