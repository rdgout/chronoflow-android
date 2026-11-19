package net.chronoflow.feature.auth.shell

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EmailAppLauncherTest {
    @Test fun startsTheNativeEmailSelectorAsANewTask() {
        var started: Intent? = null
        val context =
            object : ContextWrapper(RuntimeEnvironment.getApplication()) {
                override fun startActivity(intent: Intent) {
                    started = intent
                }
            }
        assertTrue(EmailAppLauncher(context).open())
        val intent = checkNotNull(started)
        assertEquals(Intent.ACTION_MAIN, intent.action)
        assertEquals(setOf(Intent.CATEGORY_APP_EMAIL), intent.selector!!.categories)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test fun missingHandlerReturnsFalse() {
        val context =
            object : ContextWrapper(RuntimeEnvironment.getApplication()) {
                override fun startActivity(intent: Intent): Unit = throw ActivityNotFoundException()
            }
        assertFalse(EmailAppLauncher(context).open())
    }
}
