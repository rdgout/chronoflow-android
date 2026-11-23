package net.chronoflow.debug.reminders

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.app.GraphOverrides
import net.chronoflow.app.SoftwareSessionCipher
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.AndroidAccountDriverFactory
import net.chronoflow.core.database.store.fetchSetting
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.datastore.DevicePreferences
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.debug.TestSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class RemindersSeedTest(
    private val rawPermission: String,
    private val expected: ReminderPermission,
) {
    private fun seed(testId: Boolean = true): RemindersSeed {
        val raw =
            mutableMapOf(
                "cf.notificationAuthorization" to rawPermission,
                "cf.notificationPermissionRequested" to "1",
                "cf.firstTripReminders" to "1",
            )
        if (testId) raw["CF_AUTH_TEST_ID"] = "01900000-0000-7000-8000-000000000001"
        return RemindersSeed.from(TestSeed.parse(raw))
    }

    @Test fun authorizationInstallsTheExpectedOverride() =
        runTest(timeout = 60.seconds) {
            val overrides =
                GraphOverrides(
                    sessionCipher = SoftwareSessionCipher(),
                    reminderPermission = seed().permission,
                )
            assertEquals(expected, overrides.reminderPermission?.permission())
        }

    @Test fun keysWithoutATestIdInstallNoOverride() =
        runTest(
            timeout = 60.seconds,
        ) { assertNull(seed(false).permission) }

    @Test fun requestedFlagIsWrittenBeforeLaunch() =
        runTest(timeout = 60.seconds) {
            val app = ApplicationProvider.getApplicationContext<Application>()
            val preferences = DevicePreferences.shared(app)
            preferences.setNotificationPermissionRequested(false)
            seed().apply(preferences)
            assertTrue(preferences.notificationPermissionRequested.first())
        }

    @Test fun firstTripSeedQueuesThePendingRow() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            val root = TestRoots.temporaryStoreRoot()
            val database =
                AccountDatabase(
                    root,
                    driverFactory =
                        AndroidAccountDriverFactory(
                            ApplicationProvider.getApplicationContext<Application>(),
                        ),
                    io = io,
                )
            try {
                database.open("01900000-0000-7000-8000-000000000001")
                seed().queue(database)
                assertEquals("\"pending\"", database.read { it.fetchSetting("wizardReminders")?.value })
            } finally {
                withContext(NonCancellable) {
                    database.close()
                    check(root.deleteRecursively())
                }
            }
        }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "authorization={0}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("undetermined", ReminderPermission.Undetermined),
                arrayOf("granted", ReminderPermission.Granted),
                arrayOf("blocked", ReminderPermission.Blocked),
                arrayOf("unsupported", ReminderPermission.Unsupported),
                arrayOf("bogus", ReminderPermission.Undetermined),
            )
    }
}
