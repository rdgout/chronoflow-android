package net.chronoflow.app.reminders

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.ioViolationsForTest
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.datastore.DevicePreferences
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class RemindersPermissionDeviceTest : ScreenTest() {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Before fun notificationPermissionMustBeDenied() {
        assertEquals(
            "run pm revoke before this class (see Makefile)",
            PackageManager.PERMISSION_DENIED,
            ContextCompat.checkSelfPermission(instrumentation.targetContext, Manifest.permission.POST_NOTIFICATIONS),
        )
    }

    @Test fun askOpensTheSystemDialogAndTurnsRemindersOn() {
        val started = System.nanoTime()
        launch(
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_SETTINGS_ROUTE" to "reminders",
                "cf.notificationPermissionRequested" to "0",
                "cf.settings.language" to "en",
                "cf.reduceMotion" to "1",
                "CF_MOCK_DELAY" to "0",
            ),
        )
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        compose.waitUntilExactlyOneExists(hasTestTag("reminders.ask"), 5_000)
        compose.onNodeWithTag("reminders.enabled").assertDoesNotExist()
        assertFalse(device.hasObject(By.pkg("com.android.permissioncontroller")))
        compose.onNodeWithTag("reminders.ask").performClick()
        checkNotNull(
            device.wait(Until.findObject(By.res("com.android.permissioncontroller", "permission_allow_button")), 5_000),
        ) {
            "notification permission dialog did not appear"
        }.click()
        compose.waitUntilExactlyOneExists(hasTestTag("reminders.enabled"), 5_000)
        compose.onNodeWithTag("reminders.enabled").assertIsOn()
        compose.onNodeWithTag("reminders.lead").assertExists()
        assertEquals(
            PackageManager.PERMISSION_GRANTED,
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS),
        )
        assertTrue(
            runBlocking(Dispatchers.IO) { DevicePreferences.shared(context).notificationPermissionRequested.first() },
        )
        instrumentation.runOnMainSync {
            assertEquals(0, (context.applicationContext as ChronoFlowApp).graph.ioViolationsForTest)
        }
        assertTrue("permission journey exceeded 10 seconds", System.nanoTime() - started < 10_000_000_000L)
    }
}
