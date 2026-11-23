package net.chronoflow.app.reminders

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.data.reminders.ReminderPermissions
import net.chronoflow.core.datastore.DevicePreferences
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.reminders.PermissionRequestStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class PermissionRequestStartTest {
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @Test fun belowThirtyThreeRecordsTheOptInWithoutADialog() =
        runTest(timeout = 60.seconds) {
            val preferences = DevicePreferences.shared(app)
            preferences.setNotificationPermissionRequested(false)
            val source = ReminderPermissions(app, preferences, Dispatchers.IO, 32)
            assertEquals(ReminderPermission.Undetermined, source.refresh())
            assertEquals(
                PermissionRequestStart.NoDialog,
                beginReminderPermissionRequest(
                    preferences,
                    Dispatchers.IO,
                    32,
                    null,
                ) {
                    true
                },
            )
            assertTrue(preferences.notificationPermissionRequested.first())
            assertEquals(ReminderPermission.Granted, source.refresh())
        }

    @Test fun thirtyThreeAndAboveRecordTheRequestBeforeTheDialog() =
        runTest(timeout = 60.seconds) {
            val preferences = DevicePreferences.shared(app)
            preferences.setNotificationPermissionRequested(false)
            val start = beginReminderPermissionRequest(preferences, Dispatchers.IO, 36, null) { false }
            assertEquals(PermissionRequestStart.SystemDialog, start)
            assertTrue(preferences.notificationPermissionRequested.first())
        }
}
