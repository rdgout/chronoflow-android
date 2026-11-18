package net.chronoflow.core.datastore

import android.util.AtomicFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.session.SessionStore
import net.chronoflow.core.model.session.StorageNamespace
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class KeystoreSessionStore(
    val file: File,
    private val alias: String,
    private val cipher: SessionCipher,
    private val io: CoroutineDispatcher,
) : SessionStore {
    private val mutex = Mutex()

    override suspend fun read(): ByteArray? =
        withContext(io) {
            mutex.withLock {
                val atomic = AtomicFile(file)
                val bytes =
                    try {
                        atomic.readFully()
                    } catch (_: FileNotFoundException) {
                        return@withLock null
                    }
                if (bytes.size < 29 || bytes[0] != 1.toByte()) {
                    atomic.delete()
                    return@withLock null
                }
                try {
                    val sealed = SessionCipher.Sealed(bytes.copyOfRange(1, 13), bytes.copyOfRange(13, bytes.size))
                    cipher.decrypt(alias, sealed)
                } catch (_: PermanentSessionKeyFailure) {
                    atomic.delete()
                    cipher.deleteKey(alias)
                    null
                }
            }
        }

    override suspend fun write(data: ByteArray) =
        withContext(io) {
            mutex.withLock {
                file.parentFile?.mkdirs()
                val sealed =
                    try {
                        cipher.encrypt(alias, data)
                    } catch (_: PermanentSessionKeyFailure) {
                        cipher.deleteKey(alias)
                        cipher.encrypt(alias, data)
                    }
                check(sealed.iv.size == 12)
                val atomic = AtomicFile(file)
                val stream = atomic.startWrite()
                try {
                    stream.write(1)
                    stream.write(sealed.iv)
                    stream.write(sealed.ciphertext)
                    atomic.finishWrite(stream)
                } catch (e: IOException) {
                    atomic.failWrite(stream)
                    throw e
                }
            }
        }

    override suspend fun delete() =
        withContext(io) {
            mutex.withLock {
                AtomicFile(file).delete()
                cipher.deleteKey(alias)
            }
        }

    companion object {
        fun forNamespace(
            root: File,
            namespace: StorageNamespace,
            cipher: SessionCipher,
            io: CoroutineDispatcher,
        ) = KeystoreSessionStore(
            File(File(root, namespace.directoryName), "session.bin"),
            namespace.sessionKeyAlias,
            cipher,
            io,
        )
    }
}
