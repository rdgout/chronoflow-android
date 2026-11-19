package net.chronoflow.core.datastore

import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class AndroidDeviceNameTests {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun deviceNameIsTrimmed() =
        runTest(timeout = 60.seconds) {
            Settings.Global.putString(context.contentResolver, Settings.Global.DEVICE_NAME, "  Ada's Pixel\n")
            assertEquals("Ada's Pixel", androidDeviceName(context, Dispatchers.IO))
        }

    @Test fun absentNameUsesModel() =
        runTest(timeout = 60.seconds) {
            Settings.Global.putString(context.contentResolver, Settings.Global.DEVICE_NAME, null)
            assertEquals(Build.MODEL.trim(), androidDeviceName(context, Dispatchers.IO))
        }
}
