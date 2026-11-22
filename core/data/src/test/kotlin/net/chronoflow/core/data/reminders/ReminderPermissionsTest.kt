package net.chronoflow.core.data.reminders

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.datastore.DevicePreferences
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ReminderPermissionsTest {
    @Test fun runtimePermissionNotificationsAndChannelDetermineState() =
        runTest(timeout = 60.seconds) {
            val app = ApplicationProvider.getApplicationContext<Application>()
            val dispatcher = StandardTestDispatcher(testScheduler)
            val preferences = DevicePreferences(app, MemoryReminderPreferences(), dispatcher)
            val permissions = ReminderPermissions(app, preferences, dispatcher)
            val manager = checkNotNull(app.getSystemService(NotificationManager::class.java))
            assertNull(permissions.state.value)
            assertEquals(ReminderPermission.Undetermined, permissions.refresh())
            assertEquals(ReminderPermission.Undetermined, permissions.state.value)
            preferences.setNotificationPermissionRequested(true)
            assertEquals(ReminderPermission.Blocked, permissions.refresh())
            assertEquals(ReminderPermission.Blocked, permissions.state.value)
            shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
            assertEquals(ReminderPermission.Granted, permissions.refresh())
            assertEquals(ReminderPermission.Granted, permissions.state.value)
            shadowOf(manager).setNotificationsEnabled(false)
            assertEquals(ReminderPermission.Blocked, permissions.refresh())
            shadowOf(manager).setNotificationsEnabled(true)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_NONE),
            )
            assertEquals(ReminderPermission.Blocked, permissions.refresh())
            assertEquals(ReminderPermission.Blocked, permissions.state.value)
        }

    @Test fun pre33RequiresInAppOptInAndIgnoresRuntimePermission() =
        runTest(timeout = 60.seconds) {
            val app = ApplicationProvider.getApplicationContext<Application>()
            val dispatcher = StandardTestDispatcher(testScheduler)
            val preferences = DevicePreferences(app, MemoryReminderPreferences(), dispatcher)
            val permissions = ReminderPermissions(app, preferences, dispatcher, sdkInt = 32)
            assertEquals(ReminderPermission.Undetermined, permissions.refresh())
            preferences.setNotificationPermissionRequested(true)
            assertEquals(ReminderPermission.Granted, permissions.refresh())
            assertEquals(ReminderPermission.Granted, permissions.state.value)
            shadowOf(checkNotNull(app.getSystemService(NotificationManager::class.java))).setNotificationsEnabled(false)
            assertEquals(ReminderPermission.Blocked, permissions.refresh())
        }
}
