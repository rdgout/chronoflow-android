package net.chronoflow.core.ui.secure

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import net.chronoflow.core.testing.tiers.Logic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecureWindowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var host: ActivityController<ComponentActivity>

    @Before fun createHost() {
        host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
    }

    @After fun closeHost() {
        host.pause().stop().destroy()
    }

    @Test fun flagIsSetWhileShownAndClearedAfter() {
        val count = mutableIntStateOf(1)
        host.get().setContent { if (count.intValue > 0) SecureWindow(true) }
        compose.runOnIdle {
            assertSecure(true)
            count.intValue = 0
        }
        compose.runOnIdle { assertSecure(false) }
    }

    @Test fun nestedUsesKeepTheFlagUntilTheLastLeaves() {
        val count = mutableIntStateOf(2)
        host.get().setContent {
            if (count.intValue > 0) SecureWindow(true)
            if (count.intValue > 1) SecureWindow(true)
        }
        compose.runOnIdle {
            assertSecure(true)
            count.intValue = 1
        }
        compose.runOnIdle {
            assertSecure(true)
            count.intValue = 0
        }
        compose.runOnIdle { assertSecure(false) }
    }

    @Test fun disabledDoesNothing() {
        host.get().setContent { SecureWindow(false) }
        compose.runOnIdle { assertSecure(false) }
    }

    @Test fun aFlagSetElsewhereIsNotTouchedWhenUnused() {
        host.get().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        host.get().setContent { SecureWindow(false) }
        compose.runOnIdle { assertSecure(true) }
    }

    private fun assertSecure(expected: Boolean) {
        assertEquals(
            expected,
            host
                .get()
                .window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0,
        )
    }
}
