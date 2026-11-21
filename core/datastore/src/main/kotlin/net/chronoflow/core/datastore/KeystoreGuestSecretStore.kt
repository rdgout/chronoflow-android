package net.chronoflow.core.datastore

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.guest.GuestSecretStore
import net.chronoflow.core.model.session.StorageNamespace
import java.io.File
import java.lang.ref.WeakReference
import java.util.WeakHashMap

class KeystoreGuestSecretStore(
    private val dataStore: DataStore<Preferences>,
    private val alias: String,
    private val cipher: SessionCipher,
    private val io: CoroutineDispatcher,
) : GuestSecretStore {
    private val mutex = Mutex()

    override suspend fun set(
        secret: String,
        token: String,
    ) = withContext(io) {
        mutex.withLock { write(secret, token) }
    }

    override suspend fun get(token: String): String? =
        withContext(io) {
            mutex.withLock {
                if (!valid(token)) return@withLock null
                val key = stringPreferencesKey("t.$token")
                val encoded = dataStore.data.first()[key] ?: return@withLock null
                val bytes =
                    try {
                        Base64.decode(encoded, Base64.NO_WRAP)
                    } catch (_: IllegalArgumentException) {
                        byteArrayOf()
                    }
                if (bytes.size < 29 || bytes[0] != 1.toByte()) {
                    dataStore.edit { it.remove(key) }
                    return@withLock null
                }
                try {
                    cipher
                        .decrypt(
                            alias,
                            SessionCipher.Sealed(bytes.copyOfRange(1, 13), bytes.copyOfRange(13, bytes.size)),
                        ).decodeToString()
                } catch (_: PermanentSessionKeyFailure) {
                    forgetKey()
                    null
                }
            }
        }

    override suspend fun delete(token: String) =
        withContext(io) {
            mutex.withLock { remove(token) }
        }

    override suspend fun rotate(
        oldToken: String?,
        newToken: String,
        secret: String,
    ) = withContext(io) {
        mutex.withLock {
            write(secret, newToken)
            if (oldToken != null && oldToken != newToken) remove(oldToken)
        }
    }

    override suspend fun deleteAll() =
        withContext(io) {
            mutex.withLock {
                dataStore.edit { it.clear() }
                Unit
            }
        }

    private suspend fun write(
        secret: String,
        token: String,
    ) {
        require(valid(token)) { "Invalid draft token" }
        val sealed =
            try {
                cipher.encrypt(alias, secret.encodeToByteArray())
            } catch (_: PermanentSessionKeyFailure) {
                forgetKey()
                cipher.encrypt(alias, secret.encodeToByteArray())
            }
        check(sealed.iv.size == 12)
        val encoded = Base64.encodeToString(byteArrayOf(1) + sealed.iv + sealed.ciphertext, Base64.NO_WRAP)
        dataStore.edit { it[stringPreferencesKey("t.$token")] = encoded }
    }

    private suspend fun remove(token: String) {
        dataStore.edit { it.remove(stringPreferencesKey("t.$token")) }
    }

    private suspend fun forgetKey() {
        dataStore.edit { it.clear() }
        cipher.deleteKey(alias)
    }

    override fun toString(): String = "KeystoreGuestSecretStore(***)"

    companion object {
        private val instances = WeakHashMap<Context, MutableMap<StorageNamespace, KeystoreGuestSecretStore>>()

        private fun valid(token: String): Boolean = token.isNotEmpty() && token.length <= 64

        fun forNamespace(
            context: Context,
            namespace: StorageNamespace,
            cipher: SessionCipher,
            io: CoroutineDispatcher,
        ): KeystoreGuestSecretStore {
            val app = context.applicationContext
            return synchronized(instances) {
                instances.getOrPut(app) { mutableMapOf() }.getOrPut(namespace) {
                    val application = WeakReference(app)
                    val dataStore =
                        PreferenceDataStoreFactory.create(
                            scope = CoroutineScope(SupervisorJob() + io),
                            produceFile = {
                                File(
                                    checkNotNull(application.get()).filesDir,
                                    "datastore/guest_secrets_${namespace.directoryName.lowercase()}.preferences_pb",
                                )
                            },
                        )
                    KeystoreGuestSecretStore(dataStore, namespace.guestKeyAlias, cipher, io)
                }
            }
        }
    }
}
