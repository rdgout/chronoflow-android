package net.chronoflow.core.model.guest

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class GuestSecretStoreTests {
    @Test
    fun rotationStoresNewSecretAndDeletesOnlyOldToken() =
        runTest(timeout = 60.seconds) {
            val store: GuestSecretStore = MemoryGuestSecretStore()
            store.set("old-secret", "old")
            store.set("unrelated-secret", "unrelated")
            store.rotate("old", "new", "new-secret")
            assertNull(store.get("old"))
            assertEquals("new-secret", store.get("new"))
            assertEquals("unrelated-secret", store.get("unrelated"))
        }

    @Test
    fun firstMintAndSameTokenRotationRetainNewSecret() =
        runTest(timeout = 60.seconds) {
            val store: GuestSecretStore = MemoryGuestSecretStore()
            store.rotate(null, "token", "first")
            assertEquals("first", store.get("token"))
            store.rotate("token", "token", "replacement")
            assertEquals("replacement", store.get("token"))
        }

    @Test
    fun discardIsPerTokenAndIdempotent() =
        runTest(timeout = 60.seconds) {
            val store: GuestSecretStore = MemoryGuestSecretStore()
            store.set("discarded", "discard")
            store.set("retained", "retain")
            store.delete("discard")
            store.delete("discard")
            assertNull(store.get("discard"))
            assertEquals("retained", store.get("retain"))
        }

    @Test
    fun deleteAllRemovesEveryTokenAndAllowsFreshMint() =
        runTest(timeout = 60.seconds) {
            val store: GuestSecretStore = MemoryGuestSecretStore()
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
