package net.chronoflow.core.datastore

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class KeystoreGuestSecretStoreTest {
    @Test fun rotationStoresNewSecretAndDeletesOnlyOldToken() =
        runTest(timeout = 60.seconds) {
            withStore {
                store.set("old-secret", "old")
                store.set("unrelated-secret", "unrelated")
                store.rotate("old", "new", "new-secret")
                assertNull(store.get("old"))
                assertEquals("new-secret", store.get("new"))
                assertEquals("unrelated-secret", store.get("unrelated"))
            }
        }

    @Test fun firstMintAndSameTokenRotationRetainNewSecret() =
        runTest(timeout = 60.seconds) {
            withStore {
                store.rotate(null, "token", "first")
                assertEquals("first", store.get("token"))
                store.rotate("token", "token", "replacement")
                assertEquals("replacement", store.get("token"))
            }
        }

    @Test fun discardIsPerTokenAndIdempotent() =
        runTest(timeout = 60.seconds) {
            withStore {
                store.set("discarded", "discard")
                store.set("retained", "retain")
                store.delete("discard")
                store.delete("discard")
                assertNull(store.get("discard"))
                assertEquals("retained", store.get("retain"))
            }
        }

    @Test fun deleteAllRemovesEveryTokenAndAllowsFreshMint() =
        runTest(timeout = 60.seconds) {
            withStore {
                store.set("first-secret", "first")
                store.set("second-secret", "second")
                store.deleteAll()
                store.deleteAll()
                assertNull(store.get("first"))
                assertNull(store.get("second"))
                store.set("fresh-secret", "fresh")
                assertEquals("fresh-secret", store.get("fresh"))
            }
        }

    @Test fun fileHoldsNoPlaintextSecret() =
        runTest(timeout = 60.seconds) {
            withStore {
                store.set("minted-secret", "token")
                assertFalse(file.readBytes().decodeToString().contains("minted-secret"))
                assertEquals("minted-secret", store.get("token"))
            }
        }

    @Test fun aliasIsNamespaced() =
        runTest(timeout = 60.seconds) {
            assertEquals("net.chronoflow.app.native.guest", StorageNamespace.Api.guestKeyAlias)
            assertEquals("net.chronoflow.app.native.mock.guest", StorageNamespace.Mock.guestKeyAlias)
            withStore {
                store.set("secret", "token")
                assertEquals(setOf(StorageNamespace.Mock.guestKeyAlias), cipher.delegate.keys.keys)
            }
        }

    @Test fun permanentKeyFailureForgetsEverySecret() =
        runTest(timeout = 60.seconds) {
            withStore {
                store.set("first", "a")
                store.set("second", "b")
                cipher.delegate.failNext = PermanentSessionKeyFailure()
                assertNull(store.get("a"))
                assertNull(store.get("b"))
                assertEquals(listOf(StorageNamespace.Mock.guestKeyAlias), cipher.deletions)
                assertTrue(
                    dataStore.data
                        .first()
                        .asMap()
                        .isEmpty(),
                )
            }
        }

    @Test fun permanentEncryptFailureRetriesOnceAndForgetsOldEntries() =
        runTest(timeout = 60.seconds) {
            withStore {
                store.set("old", "old")
                cipher.delegate.failNext = PermanentSessionKeyFailure()
                store.set("fresh", "fresh")
                assertNull(store.get("old"))
                assertEquals("fresh", store.get("fresh"))
                assertEquals(1, cipher.deletions.size)
                cipher.alwaysFail = true
                try {
                    store.set("fails", "failed")
                    fail("a second permanent failure must propagate")
                } catch (_: PermanentSessionKeyFailure) {
                    assertEquals(2, cipher.failedEncryptions)
                }
            }
        }

    @Test fun badVersionByteDropsOnlyThatEntry() =
        runTest(timeout = 60.seconds) {
            withStore {
                store.set("retained", "retain")
                dataStore.edit {
                    it[stringPreferencesKey("t.bad")] =
                        Base64.encodeToString(ByteArray(30) { 2 }, Base64.NO_WRAP)
                }
                assertNull(store.get("bad"))
                assertEquals("retained", store.get("retain"))
                assertNull(dataStore.data.first()[stringPreferencesKey("t.bad")])
            }
        }

    @Test fun rejectsEmptyAndOverlongTokens() =
        runTest(timeout = 60.seconds) {
            withStore {
                for (token in listOf("", "t".repeat(65))) {
                    assertNull(store.get(token))
                    try {
                        store.set("secret", token)
                        fail("invalid token accepted")
                    } catch (
                        _: IllegalArgumentException,
                    ) {
                    }
                    try {
                        store.rotate("old", token, "secret")
                        fail("invalid rotation accepted")
                    } catch (
                        _: IllegalArgumentException,
                    ) {
                    }
                }
                store.set("boundary", "t".repeat(64))
                assertEquals("boundary", store.get("t".repeat(64)))
            }
        }

    @Test fun runsOnTheInjectedDispatcher() =
        runTest(timeout = 60.seconds) {
            withStore {
                val inlineStore =
                    KeystoreGuestSecretStore(
                        dataStore,
                        StorageNamespace.Mock.guestKeyAlias,
                        cipher,
                        UnconfinedTestDispatcher(testScheduler),
                    )
                val control = launch(start = CoroutineStart.UNDISPATCHED) { inlineStore.get("") }
                assertTrue("the guard distinguishes a missing dispatcher hop", control.isCompleted)
                val pending = launch(start = CoroutineStart.UNDISPATCHED) { store.get("") }
                assertFalse(pending.isCompleted)
                runCurrent()
                assertTrue(pending.isCompleted)
            }
        }

    private class RecordingCipher : SessionCipher {
        val delegate = SoftwareSessionCipher()
        val deletions = mutableListOf<String>()
        var alwaysFail = false
        var failedEncryptions = 0

        override fun encrypt(
            alias: String,
            plaintext: ByteArray,
        ): SessionCipher.Sealed {
            if (alwaysFail) {
                failedEncryptions++
                throw PermanentSessionKeyFailure()
            }
            return delegate.encrypt(alias, plaintext)
        }

        override fun decrypt(
            alias: String,
            sealed: SessionCipher.Sealed,
        ) = delegate.decrypt(alias, sealed)

        override fun deleteKey(alias: String) {
            deletions += alias
            delegate.deleteKey(alias)
        }
    }

    private class Fixture(
        val dataStore: DataStore<Preferences>,
        val file: File,
        val store: KeystoreGuestSecretStore,
        val cipher: RecordingCipher,
    )

    private suspend fun TestScope.withStore(block: suspend Fixture.() -> Unit) {
        val root = Files.createTempDirectory("guest-secrets").toFile()
        val file = File(root, "guest.preferences_pb")
        val dispatcher = StandardTestDispatcher(testScheduler)
        val job = SupervisorJob(backgroundScope.coroutineContext[Job])
        val dataStore =
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(job + dispatcher),
                produceFile = { file },
            )
        val cipher = RecordingCipher()
        val store = KeystoreGuestSecretStore(dataStore, StorageNamespace.Mock.guestKeyAlias, cipher, dispatcher)
        try {
            Fixture(dataStore, file, store, cipher).block()
        } finally {
            withContext(NonCancellable) {
                job.cancelAndJoin()
                root.deleteRecursively()
            }
        }
    }
}
