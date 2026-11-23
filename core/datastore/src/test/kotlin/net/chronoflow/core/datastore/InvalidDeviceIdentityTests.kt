package net.chronoflow.core.datastore

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.device.DeviceIdentityFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class InvalidDeviceIdentityTests(
    private val value: String,
) {
    @Test fun corruptedPreferencesDoNotRotateDeviceIdentity() =
        runTest(timeout = 60.seconds) {
            val preferences = IdentityMemoryPreferences(value)
            try {
                DeviceIdentityStore(preferences).identity()
                fail()
            } catch (
                _: DeviceIdentityFailure.InvalidPersistedIdentity,
            ) {
            }
            assertEquals(0, preferences.writes)
            assertEquals(value, preferences.value)
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}")
        fun cases() =
            listOf(
                "garbage",
                "123e4567-e89b-42d3-a456-426614174000",
                "00000000-0000-7000-0000-000000000000",
            )
    }
}
