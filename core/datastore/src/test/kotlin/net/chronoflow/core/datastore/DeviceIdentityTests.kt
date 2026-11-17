package net.chronoflow.core.datastore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.device.DeviceIdentityPersistence
import net.chronoflow.core.model.session.StorageNamespace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

internal class IdentityMemoryPreferences(
    var value: String? = null,
) : DeviceIdentityPersistence {
    var writes = 0
    var writeFails = false

    override suspend fun readDeviceID() = value

    override suspend fun writeDeviceID(value: String) {
        if (writeFails) error("unavailable")
        this.value = value
        writes++
    }
}

class DeviceIdentityTests {
    @Test fun concurrentCreationPersistsOnceAndRestartAndAccountWipeReuseIdentity() =
        runTest(timeout = 60.seconds) {
            val preferences = IdentityMemoryPreferences()
            val store = DeviceIdentityStore(preferences, { 1_700_000_000.123 }, { ByteArray(10) { it.toByte() } })
            val results = List(40) { async(Dispatchers.Default) { store.identity() } }.awaitAll()
            assertEquals(1, results.toSet().size)
            assertTrue(results.first().startsWith("018bcfe5-687b"))
            assertEquals(1, preferences.writes)
            assertEquals(results[0], DeviceIdentityStore(preferences).identity())
            assertNotEquals(StorageNamespace.Api.directoryName, StorageNamespace.Mock.directoryName)
            assertEquals("net.chronoflow.app.native", StorageNamespace.Api.keychainService)
            assertEquals("net.chronoflow.app.native.mock", StorageNamespace.Mock.keychainService)
            assertEquals(results[0], store.identity())
        }

    @Test fun failedPersistenceRetriesWithoutCachingAnUnpersistedID() =
        runTest(timeout = 60.seconds) {
            val persistence = IdentityMemoryPreferences().apply { writeFails = true }
            val store = DeviceIdentityStore(persistence)
            try {
                store.identity()
                fail()
            } catch (_: IllegalStateException) {
            }
            assertNull(persistence.value)
            persistence.writeFails = false
            assertEquals(store.identity(), persistence.value)
            assertEquals(1, persistence.writes)
        }

    @Test fun deviceNameTrimsAndLimitsUnicodeCharactersWithModelFallback() {
        assertEquals("Ada's Pixel", DeviceIdentityStore.deviceName("  Ada's Pixel\n", "Pixel 8"))
        assertEquals("Pixel 8", DeviceIdentityStore.deviceName(" \n ", " Pixel 8 "))
        val family = "👨‍👩‍👧‍👦"
        val result = DeviceIdentityStore.deviceName(family.repeat(256), "Pixel")
        assertEquals(family.repeat(36), result)
        assertEquals(252, result.codePointCount(0, result.length))
    }

    @Test fun upperCasePersistedIdentityIsLowerCased() =
        runTest(timeout = 60.seconds) {
            val id = "019a562b-8000-7000-8000-00000000000a"
            assertEquals(id, DeviceIdentityStore(IdentityMemoryPreferences(id.uppercase())).identity())
        }

    @Test fun invalidRandomnessFailsBeforePersistence() =
        runTest(timeout = 60.seconds) {
            val preferences = IdentityMemoryPreferences()
            try {
                DeviceIdentityStore(preferences, { 1.0 }, { byteArrayOf() }).identity()
                fail()
            } catch (
                _: DeviceIdentityFailure.InvalidRandomness,
            ) {
            }
            assertEquals(0, preferences.writes)
        }
}
