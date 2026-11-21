package net.chronoflow.core.model.guest

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.chronoflow.core.model.session.StorageNamespace
import java.io.File

interface GuestSecretStore {
    suspend fun set(
        secret: String,
        token: String,
    )

    suspend fun get(token: String): String?

    suspend fun delete(token: String)

    suspend fun rotate(
        oldToken: String?,
        newToken: String,
        secret: String,
    )

    suspend fun deleteAll()
}

class MemoryGuestSecretStore : GuestSecretStore {
    private val mutex = Mutex()
    private val secrets = HashMap<String, String>()

    override suspend fun set(
        secret: String,
        token: String,
    ) = mutex.withLock { secrets[token] = secret }

    override suspend fun get(token: String): String? = mutex.withLock { secrets[token] }

    override suspend fun delete(token: String) {
        mutex.withLock { secrets.remove(token) }
    }

    override suspend fun rotate(
        oldToken: String?,
        newToken: String,
        secret: String,
    ) {
        mutex.withLock {
            secrets[newToken] = secret
            if (oldToken != null && oldToken != newToken) secrets.remove(oldToken)
        }
    }

    override suspend fun deleteAll() = mutex.withLock { secrets.clear() }

    override fun toString(): String = "MemoryGuestSecretStore(***)"
}

object GuestRecoveryLocation {
    fun file(
        noBackupRoot: File,
        namespace: StorageNamespace,
    ): File = File(File(noBackupRoot, namespace.directoryName), "guest-wizard.json")
}
