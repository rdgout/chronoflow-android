package net.chronoflow.core.data.session

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.chronoflow.core.datastore.CorruptWipeMarkerException
import net.chronoflow.core.datastore.WipeMarker
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.session.SessionStore
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.store.StoreFailure
import java.io.File

class AccountWiper(
    private val storageRoot: suspend () -> File,
    private val namespace: StorageNamespace,
    private val credentials: SessionStore,
    private val guestSecrets: GuestSecrets = NoGuestSecrets,
    private val io: CoroutineDispatcher,
    private val remove: (File) -> Unit = {
        if (it.exists() &&
            !it.deleteRecursively()
        ) {
            throw StoreFailure.Unavailable()
        }
    },
) {
    constructor(
        storageRoot: File,
        namespace: StorageNamespace,
        credentials: SessionStore,
        guestSecrets: GuestSecrets = NoGuestSecrets,
        io: CoroutineDispatcher,
        remove: (File) -> Unit = { if (it.exists() && !it.deleteRecursively()) throw StoreFailure.Unavailable() },
    ) : this({ storageRoot }, namespace, credentials, guestSecrets, io, remove)

    private suspend fun root() = File(storageRoot(), namespace.directoryName)

    private suspend fun marker() = WipeMarker(root(), io)

    suspend fun begin(id: AccountID) = withContext(io) { marker().write(id) }

    suspend fun recover() =
        withContext(io) {
            val id =
                try {
                    marker().read()
                } catch (
                    _: CorruptWipeMarkerException,
                ) {
                    throw SessionFailure.DatabaseUnavailable
                }
            if (id != null) {
                erase(id)
                credentials.delete()
                finish()
            }
        }

    suspend fun erase(id: AccountID) =
        withContext(io) {
            remove(File(root(), "Accounts/${id.rawValue}"))
            guestSecrets.deleteAll()
            remove(File(root(), "guest-wizard.json"))
        }

    suspend fun finish() = withContext(io) { marker().clear() }
}
