package net.chronoflow.app.reminders

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class NotificationSettingsIntentsTest {
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @Test fun appLevelIncludesThePackage() {
        val intent = notificationSettingsIntent(app.packageName, false, true)
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.action)
        assertEquals(app.packageName, intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
    }

    @Test fun onlyChannelBlockedOpensTheChannel() {
        val intent = notificationSettingsIntent(app.packageName, true, true)
        assertEquals(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS, intent.action)
        assertEquals("reminders", intent.getStringExtra(Settings.EXTRA_CHANNEL_ID))
        assertEquals(app.packageName, intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
    }

    @Test fun missingNotificationSettingsFallsBackToApplicationDetails() {
        val fallback =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.packageName, null))
        val component = ComponentName("android", "Details")
        shadowOf(app.packageManager).addActivityIfNotPresent(component).exported = true
        shadowOf(app.packageManager).addIntentFilterForActivity(
            component,
            IntentFilter(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                addDataScheme("package")
                addCategory(Intent.CATEGORY_DEFAULT)
            },
        )
        shadowOf(app).checkActivities(true)
        startNotificationSettings(app, notificationSettingsIntent(app.packageName, false, false))
        val started = shadowOf(app).nextStartedActivity
        assertEquals(fallback.action, started.action)
        assertEquals(fallback.data, started.data)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, started.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
