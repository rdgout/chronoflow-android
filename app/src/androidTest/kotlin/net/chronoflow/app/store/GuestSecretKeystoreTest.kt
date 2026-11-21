package net.chronoflow.app.store

import android.security.keystore.KeyInfo
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.datastore.AndroidKeystoreSessionCipher
import net.chronoflow.core.datastore.KeystoreGuestSecretStore
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.security.KeyStore
import java.util.UUID
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import kotlin.time.Duration.Companion.seconds

@Smoke
class GuestSecretKeystoreTest {
    private val alias = "test.${UUID.randomUUID()}.guest"
    private val cipher = AndroidKeystoreSessionCipher()
    private val job = SupervisorJob()
    private lateinit var root: File
    private lateinit var store: KeystoreGuestSecretStore

    @Before fun prepare() {
        root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "guest-${UUID.randomUUID()}")
        val preferences =
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(job + Dispatchers.IO),
                produceFile = { File(root, "guest.preferences_pb") },
            )
        store = KeystoreGuestSecretStore(preferences, alias, cipher, Dispatchers.IO)
    }

    @After fun cleanup() =
        runTest(timeout = 60.seconds) {
            job.cancelAndJoin()
            cipher.deleteKey(alias)
            root.deleteRecursively()
        }

    @Test fun testGuestSecretsRotateDiscardAndClearWithDeviceOnlyAccessibility() =
        runTest(timeout = 60.seconds) {
            store.set("first", "old")
            store.set("unrelated", "retain")
            store.rotate("old", "new", "replacement")
            assertNull(store.get("old"))
            assertEquals("replacement", store.get("new"))
            assertEquals("unrelated", store.get("retain"))
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val key = keyStore.getKey(alias, null) as SecretKey
            val info =
                SecretKeyFactory
                    .getInstance(
                        key.algorithm,
                        "AndroidKeyStore",
                    ).getKeySpec(key, KeyInfo::class.java) as KeyInfo
            assertFalse(info.isUserAuthenticationRequired)
            assertEquals(alias, info.keystoreAlias)
            store.delete("new")
            store.delete("new")
            assertNull(store.get("new"))
            store.deleteAll()
            assertNull(store.get("retain"))
            assertTrue("deleteAll keeps the encryption key", keyStore.containsAlias(alias))
            store.set("fresh", "fresh")
            assertEquals("fresh", store.get("fresh"))
        }
}
