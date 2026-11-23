package net.chronoflow.app

import android.app.Application
import android.content.ComponentName
import android.content.pm.ActivityInfo
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class MainActivityConfigChangesTest {
    @Test fun manifestHandlesOnlyTheInPlaceChanges() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val actual =
            context.packageManager
                .getActivityInfo(
                    ComponentName(context, MainActivity::class.java),
                    0,
                ).configChanges
        val expected =
            ActivityInfo.CONFIG_ORIENTATION or ActivityInfo.CONFIG_SCREEN_SIZE or
                ActivityInfo.CONFIG_SCREEN_LAYOUT or ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE or
                ActivityInfo.CONFIG_DENSITY or ActivityInfo.CONFIG_UI_MODE or ActivityInfo.CONFIG_KEYBOARD or
                ActivityInfo.CONFIG_KEYBOARD_HIDDEN or ActivityInfo.CONFIG_NAVIGATION
        val recreation =
            ActivityInfo.CONFIG_LOCALE or ActivityInfo.CONFIG_FONT_SCALE or ActivityInfo.CONFIG_LAYOUT_DIRECTION
        assertEquals(expected, actual and expected)
        assertEquals(0, actual and recreation)
    }
}
