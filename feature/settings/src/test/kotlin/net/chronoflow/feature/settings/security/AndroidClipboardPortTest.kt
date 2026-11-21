package net.chronoflow.feature.settings.security

import android.content.ClipDescription
import android.content.ClipboardManager
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33, 36])
class AndroidClipboardPortTest {
    @Test fun sensitiveLabelAndClear() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val manager = checkNotNull(context.getSystemService(ClipboardManager::class.java))
        val port = AndroidClipboardPort(manager)
        val timestamp = port.set("ChronoFlow", "secret", true)
        val description = checkNotNull(manager.primaryClipDescription)
        assertTrue(checkNotNull(description.extras).getBoolean(ClipDescription.EXTRA_IS_SENSITIVE))
        assertEquals("ChronoFlow", description.label)
        assertEquals(timestamp, port.currentTimestamp())
        port.clear()
        assertNull(manager.primaryClip)
    }
}
