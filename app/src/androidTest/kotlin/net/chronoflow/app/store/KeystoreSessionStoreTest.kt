package net.chronoflow.app.store

import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.datastore.AndroidKeystoreSessionCipher
import net.chronoflow.core.datastore.KeystoreSessionStore
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.model.session.loadSession
import net.chronoflow.core.model.session.readStoredSession
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.security.KeyStore
import java.util.UUID
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import kotlin.time.Duration.Companion.seconds

@Smoke
class KeystoreSessionStoreTest {
    private val suffix = UUID.randomUUID().toString()
    private val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "keystore-$suffix")
    private val cipher = AndroidKeystoreSessionCipher()
    private val apiAlias = "test.$suffix.api.session"
    private val mockAlias = "test.$suffix.mock.session"
    private val api = KeystoreSessionStore(File(root, "api/session.bin"), apiAlias, cipher, Dispatchers.IO)
    private val mock = KeystoreSessionStore(File(root, "mock/session.bin"), mockAlias, cipher, Dispatchers.IO)

    @After fun cleanup() {
        cipher.deleteKey(apiAlias)
        cipher.deleteKey(mockAlias)
        root.deleteRecursively()
    }

    @Test fun hostedKeystoreRoundTripAndMalformedDeletion() =
        runTest(timeout = 60.seconds) {
            api.delete()
            assertNull(api.read())
            api.write(StoredSession("test", "account").encode())
            assertEquals(SessionPresence.SignedIn("account"), api.loadSession())
            api.write("invalid".encodeToByteArray())
            assertEquals(SessionPresence.SignedOut, api.loadSession())
            assertNull(api.read())
            assertFalse(api.file.exists())
        }

    @Test fun legacyShapeKeyPropertiesAndSourceNamespaces() =
        runTest(timeout = 60.seconds) {
            api.write(
                """{"token":"legacy-native","accountId":"019a562b-8000-7000-8000-000000000001"}""".encodeToByteArray(),
            )
            assertEquals("legacy-native", api.readStoredSession()?.token)
            assertNull(mock.readStoredSession())
            mock.write("""{"token":"mock","accountId":"019a562b-8000-7000-8000-000000000002"}""".encodeToByteArray())
            assertEquals("legacy-native", api.readStoredSession()?.token)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val key = keyStore.getKey(apiAlias, null) as SecretKey
            val info =
                SecretKeyFactory
                    .getInstance(
                        key.algorithm,
                        "AndroidKeyStore",
                    ).getKeySpec(key, KeyInfo::class.java) as KeyInfo
            assertEquals(256, info.keySize)
            assertFalse(info.isUserAuthenticationRequired)
            assertEquals(KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT, info.purposes)
            assertEquals(listOf(KeyProperties.BLOCK_MODE_GCM), info.blockModes.toList())
            assertEquals(listOf(KeyProperties.ENCRYPTION_PADDING_NONE), info.encryptionPaddings.toList())
            api.delete()
            assertEquals("mock", mock.readStoredSession()?.token)
        }

    @Test fun lostKeyDeletesTheSession() =
        runTest(timeout = 60.seconds) {
            api.write(StoredSession("test", "account").encode())
            cipher.deleteKey(apiAlias)
            assertNull(api.read())
            assertFalse(api.file.exists())
        }

    @Test fun tamperedFileDeletesTheSession() =
        runTest(timeout = 60.seconds) {
            api.write(StoredSession("test", "account").encode())
            val bytes = api.file.readBytes()
            bytes[13] = (bytes[13].toInt() xor 1).toByte()
            api.file.writeBytes(bytes)
            assertNull(api.read())
            assertFalse(api.file.exists())
        }

    @Test fun swappedAliasFileDeletesTheSession() =
        runTest(timeout = 60.seconds) {
            api.write(StoredSession("test", "account").encode())
            mock.write(StoredSession("other", "account").encode())
            mock.file.copyTo(api.file, overwrite = true)
            assertNull(api.read())
            assertFalse(api.file.exists())
            assertNotNull(mock.read())
        }
}
