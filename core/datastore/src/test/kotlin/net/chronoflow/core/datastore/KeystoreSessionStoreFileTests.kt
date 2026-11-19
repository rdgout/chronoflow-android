package net.chronoflow.core.datastore

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.testing.tiers.Logic
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.security.ProviderException
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class KeystoreSessionStoreFileTests {
    private val root = Files.createTempDirectory("sessions").toFile()
    private val cipher = SoftwareSessionCipher()
    private val store = KeystoreSessionStore.forNamespace(root, StorageNamespace.Api, cipher, Dispatchers.IO)

    @After fun cleanup() {
        root.deleteRecursively()
    }

    @Test fun roundTripWritesTheVersionedFormat() =
        runTest(timeout = 60.seconds) {
            val plaintext = "unique-session-secret".encodeToByteArray()
            store.write(plaintext)
            val bytes = store.file.readBytes()
            assertEquals(1.toByte(), bytes[0])
            assertEquals(29 + plaintext.size, bytes.size)
            assertFalse(bytes.toList().windowed(plaintext.size).any { it == plaintext.toList() })
            assertArrayEquals(plaintext, store.read())
        }

    @Test fun missingFileReadsNull() = runTest(timeout = 60.seconds) { assertNull(store.read()) }

    @Test fun truncatedOrUnknownVersionIsDeleted() =
        runTest(timeout = 60.seconds) {
            store.write(byteArrayOf(1))
            store.file.writeBytes(byteArrayOf(1))
            assertNull(store.read())
            assertFalse(store.file.exists())
            store.write(byteArrayOf(1))
            val bytes = store.file.readBytes()
            bytes[0] = 2
            store.file.writeBytes(bytes)
            assertNull(store.read())
            assertFalse(store.file.exists())
        }

    @Test fun permanentKeyFailureDeletesFileAndAlias() =
        runTest(timeout = 60.seconds) {
            store.write(byteArrayOf(1))
            cipher.failNext = PermanentSessionKeyFailure()
            assertNull(store.read())
            assertFalse(store.file.exists())
            assertTrue(cipher.keys.isEmpty())
        }

    @Test fun transientFailureThrowsAndKeepsTheFile() =
        runTest(timeout = 60.seconds) {
            store.write(byteArrayOf(1))
            val bytes = store.file.readBytes()
            cipher.failNext = ProviderException()
            try {
                store.read()
                fail()
            } catch (_: ProviderException) {
            }
            assertArrayEquals(bytes, store.file.readBytes())
            cipher.failNext = IOException()
            try {
                store.read()
                fail()
            } catch (_: IOException) {
            }
            assertArrayEquals(bytes, store.file.readBytes())
        }

    @Test fun deleteRemovesTheFileAndAlias() =
        runTest(timeout = 60.seconds) {
            store.write(byteArrayOf(1))
            File(store.file.path + ".bak").writeText("backup")
            store.delete()
            assertFalse(store.file.exists())
            assertFalse(File(store.file.path + ".bak").exists())
            assertFalse(cipher.keys.containsKey(StorageNamespace.Api.sessionKeyAlias))
        }

    private class BrokenSessionCipher(
        private val delegate: SessionCipher,
        private val repairsAfterDeletion: Boolean = true,
    ) : SessionCipher by delegate {
        var encryptions = 0
        var deletions = 0
        private var broken = true

        override fun encrypt(
            alias: String,
            plaintext: ByteArray,
        ): SessionCipher.Sealed {
            encryptions++
            if (broken) throw PermanentSessionKeyFailure()
            return delegate.encrypt(alias, plaintext)
        }

        override fun deleteKey(alias: String) {
            deletions++
            if (repairsAfterDeletion) broken = false
            delegate.deleteKey(alias)
        }
    }

    @Test fun writeRepairsABrokenKeyWhileSignedOut() =
        runTest(timeout = 60.seconds) {
            val broken = BrokenSessionCipher(cipher)
            val signedOut = KeystoreSessionStore.forNamespace(root, StorageNamespace.Api, broken, Dispatchers.IO)
            assertNull(signedOut.read())
            val plaintext = "new-session".encodeToByteArray()
            signedOut.write(plaintext)
            assertEquals(2, broken.encryptions)
            assertEquals(1, broken.deletions)
            assertArrayEquals(plaintext, signedOut.read())
        }

    @Test fun writeRetriesAPermanentKeyFailureOnlyOnce() =
        runTest(timeout = 60.seconds) {
            val broken = BrokenSessionCipher(cipher, repairsAfterDeletion = false)
            val signedOut = KeystoreSessionStore.forNamespace(root, StorageNamespace.Api, broken, Dispatchers.IO)
            try {
                signedOut.write("new-session".encodeToByteArray())
                fail()
            } catch (_: PermanentSessionKeyFailure) {
            }
            assertEquals(2, broken.encryptions)
            assertEquals(1, broken.deletions)
            assertFalse(signedOut.file.exists())
        }

    @Test fun namespacesAreIsolated() =
        runTest(timeout = 60.seconds) {
            val mock = KeystoreSessionStore.forNamespace(root, StorageNamespace.Mock, cipher, Dispatchers.IO)
            store.write(byteArrayOf(1))
            assertNull(mock.read())
            mock.write(byteArrayOf(2))
            assertArrayEquals(byteArrayOf(1), store.read())
            assertArrayEquals(byteArrayOf(2), mock.read())
            assertEquals(2, cipher.keys.size)
        }

    @Test fun wrongAliasCannotDecrypt() =
        runTest(timeout = 60.seconds) {
            val mock = KeystoreSessionStore.forNamespace(root, StorageNamespace.Mock, cipher, Dispatchers.IO)
            store.write(byteArrayOf(1))
            mock.write(byteArrayOf(2))
            mock.file.copyTo(store.file, overwrite = true)
            assertNull(store.read())
            assertFalse(store.file.exists())
        }
}
