package net.chronoflow.core.datastore

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.device.DeviceIdentityFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class InvalidDeviceClockTests(
    private val time: Double,
) {
    @Test fun invalidClockAndRandomnessFailBeforePersistence() =
        runTest(timeout = 60.seconds) {
            val preferences = IdentityMemoryPreferences()
            try {
                DeviceIdentityStore(preferences, { time }).identity()
                fail()
            } catch (
                _: DeviceIdentityFailure.InvalidTimestamp,
            ) {
            }
            assertEquals(0, preferences.writes)
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}")
        fun cases() = listOf(-1.0, Double.POSITIVE_INFINITY, Double.NaN, 2.0.pow(48))
    }
}
